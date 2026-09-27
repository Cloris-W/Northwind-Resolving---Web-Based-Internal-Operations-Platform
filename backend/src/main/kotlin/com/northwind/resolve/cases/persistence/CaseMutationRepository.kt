package com.northwind.resolve.cases.persistence

import com.northwind.resolve.cases.domain.CaseEventEntity
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.ZoneOffset

@Repository
@Profile("database")
class CaseMutationRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun updateAssignment(caseId: String, assignedTeam: String): Boolean = jdbc.update(
        "UPDATE cases SET assigned_team = :assignedTeam WHERE case_id = :caseId",
        mapOf("caseId" to caseId, "assignedTeam" to assignedTeam),
    ) == 1

    fun append(event: CaseEventEntity) {
        jdbc.update(
            """INSERT INTO case_events (event_id, case_id, event_type, source_system, occurred_at, actor, description, metadata_json)
               VALUES (:eventId, :caseId, :eventType, :sourceSystem, :occurredAt, :actor, :description, CAST(:metadataJson AS jsonb))""",
            MapSqlParameterSource(mapOf("eventId" to event.eventId, "caseId" to event.caseId, "eventType" to event.eventType.name,
                "sourceSystem" to event.sourceSystem.name, "occurredAt" to event.occurredAt.atOffset(ZoneOffset.UTC), "actor" to event.actor,
                "description" to event.description, "metadataJson" to (event.metadataJson ?: "{}"))).apply {
                addValue("occurredAt", event.occurredAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
            },
        )
    }
}
