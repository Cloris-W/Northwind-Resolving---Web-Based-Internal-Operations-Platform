package com.northwind.resolve.cases.persistence

import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseEntity
import com.northwind.resolve.cases.domain.CaseEventEntity
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.domain.SourceSystem
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

data class CaseSearchCriteria(
    val status: CaseStatus?,
    val category: CaseCategory?,
    val region: String?,
    val accountId: String?,
    val slaBreached: Boolean?,
    val page: Int,
    val size: Int,
)

data class CaseSearchPage(val items: List<CaseEntity>, val totalElements: Long)

@Repository
@Profile("database")
class CaseWorkspaceRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun search(criteria: CaseSearchCriteria): CaseSearchPage {
        val where = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any?>()
        criteria.status?.let { where += "c.status = :status"; parameters["status"] = it.name }
        criteria.category?.let { where += "c.category = :category"; parameters["category"] = it.name }
        criteria.region?.takeIf { it.isNotBlank() }?.let { where += "c.region = :region"; parameters["region"] = it }
        criteria.accountId?.takeIf { it.isNotBlank() }?.let { where += "c.account_id = :accountId"; parameters["accountId"] = it }
        criteria.slaBreached?.let {
            // The imported source fact remains encapsulated in this persistence boundary.
            where += "EXISTS (SELECT 1 FROM legacy_complaints lc WHERE lc.complaint_id = c.case_id AND lc.sla_breach = :slaBreached)"
            parameters["slaBreached"] = it
        }
        val clause = if (where.isEmpty()) "" else " WHERE ${where.joinToString(" AND ")}"
        val count = jdbc.queryForObject("SELECT COUNT(*) FROM cases c$clause", parameters, Long::class.java) ?: 0L
        parameters["limit"] = criteria.size
        parameters["offset"] = criteria.page * criteria.size
        val items = jdbc.query(
            "SELECT c.* FROM cases c$clause ORDER BY c.opened_at DESC, c.case_id ASC LIMIT :limit OFFSET :offset",
            parameters,
        ) { rs, _ -> caseFrom(rs) }
        return CaseSearchPage(items, count)
    }

    fun findCase(caseId: String): CaseEntity? = jdbc.query(
        "SELECT * FROM cases WHERE case_id = :caseId", mapOf("caseId" to caseId),
    ) { rs, _ -> caseFrom(rs) }.firstOrNull()

    fun accountExists(accountId: String): Boolean = (jdbc.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM cases WHERE account_id = :accountId)", mapOf("accountId" to accountId), Boolean::class.java,
    ) ?: false)

    fun findEvents(caseId: String): List<CaseEventEntity> = jdbc.query(
        "SELECT * FROM case_events WHERE case_id = :caseId ORDER BY occurred_at ASC, event_id ASC", mapOf("caseId" to caseId),
    ) { rs, _ ->
        CaseEventEntity(
            eventId = rs.getObject("event_id", UUID::class.java), caseId = rs.getString("case_id"),
            eventType = CaseEventType.valueOf(rs.getString("event_type")), sourceSystem = SourceSystem.valueOf(rs.getString("source_system")),
            occurredAt = rs.getObject("occurred_at", java.time.OffsetDateTime::class.java).toInstant(), actor = rs.getString("actor"),
            description = rs.getString("description"), metadataJson = rs.getString("metadata_json"),
        )
    }

    private fun caseFrom(rs: java.sql.ResultSet) = CaseEntity(
        caseId = rs.getString("case_id"), accountId = rs.getString("account_id"), sourceSystemId = rs.getString("source_system_id"),
        category = CaseCategory.valueOf(rs.getString("category")), priority = CasePriority.valueOf(rs.getString("priority")),
        region = rs.getString("region"), status = CaseStatus.valueOf(rs.getString("status")), slaDays = rs.getInt("sla_days"),
        openedAt = rs.getObject("opened_at", java.time.OffsetDateTime::class.java).toInstant(),
        closedAt = rs.getObject("closed_at", java.time.OffsetDateTime::class.java)?.toInstant(), assignedTeam = rs.getString("assigned_team"),
    )
}
