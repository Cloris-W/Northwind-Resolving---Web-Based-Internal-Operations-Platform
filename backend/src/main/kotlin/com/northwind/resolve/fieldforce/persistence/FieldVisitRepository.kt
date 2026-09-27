package com.northwind.resolve.fieldforce.persistence

import com.northwind.resolve.fieldforce.domain.FieldVisitEntity
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.ZoneOffset

@Repository
@Profile("database")
class FieldVisitRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun create(visit: FieldVisitEntity): FieldVisitEntity {
        jdbc.update(
            """INSERT INTO field_visits (id, case_id, status, scheduled_at, completed_at, outcome)
               VALUES (:id, :caseId, :status, :scheduledAt, :completedAt, :outcome)""",
            MapSqlParameterSource(mapOf("id" to visit.id, "caseId" to visit.caseId, "status" to visit.status.name,
                "scheduledAt" to visit.scheduledAt.atOffset(ZoneOffset.UTC), "completedAt" to visit.completedAt?.atOffset(ZoneOffset.UTC), "outcome" to visit.outcome)).apply {
                addValue("scheduledAt", visit.scheduledAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                addValue("completedAt", visit.completedAt?.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
            },
        )
        return visit
    }
}
