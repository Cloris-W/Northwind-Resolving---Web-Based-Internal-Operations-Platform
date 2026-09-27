package com.northwind.resolve.billing.persistence

import com.northwind.resolve.billing.domain.BillCorrectionEntity
import com.northwind.resolve.billing.domain.BillingExceptionEntity
import com.northwind.resolve.billing.domain.BillingExceptionStatus
import com.northwind.resolve.billing.domain.RiskLevel
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
@Profile("database")
class BillingHistoryReadRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun correctionCount(accountId: String): Long = jdbc.queryForObject(
        "SELECT COUNT(*) FROM bill_corrections WHERE account_id = :accountId",
        mapOf("accountId" to accountId), Long::class.java,
    ) ?: 0L

    fun findCorrections(accountId: String, from: Instant?, to: Instant?): List<BillCorrectionEntity> {
        val conditions = mutableListOf("account_id = :accountId")
        val parameters = mutableMapOf<String, Any?>("accountId" to accountId)
        from?.let { conditions += "created_at >= :fromAt"; parameters["fromAt"] = it }
        to?.let { conditions += "created_at <= :toAt"; parameters["toAt"] = it }
        return jdbc.query("SELECT * FROM bill_corrections WHERE ${conditions.joinToString(" AND ")} ORDER BY created_at ASC, id ASC", parameters) { rs, _ -> BillCorrectionEntity(rs.getObject("id", UUID::class.java), rs.getString("account_id"), rs.getBigDecimal("original_value"), rs.getBigDecimal("corrected_value"), rs.getString("reason"), rs.getString("region"), rs.getObject("created_at", java.time.OffsetDateTime::class.java).toInstant()) }
    }

    fun findLatestException(caseId: String): BillingExceptionEntity? = jdbc.query(
        "SELECT * FROM billing_exceptions WHERE case_id = :caseId ORDER BY created_at DESC, id DESC LIMIT 1", mapOf("caseId" to caseId),
    ) { rs, _ -> BillingExceptionEntity(rs.getObject("id", UUID::class.java), rs.getString("account_id"), rs.getString("case_id"), rs.getInt("risk_score"), RiskLevel.valueOf(rs.getString("risk_level")), rs.getString("reason_codes"), BillingExceptionStatus.valueOf(rs.getString("status")), rs.getString("region"), rs.getObject("created_at", java.time.OffsetDateTime::class.java).toInstant(), rs.getString("reviewed_by")) }.firstOrNull()
}
