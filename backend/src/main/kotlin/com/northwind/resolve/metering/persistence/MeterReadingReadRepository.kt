package com.northwind.resolve.metering.persistence

import com.northwind.resolve.metering.domain.MeterReadingEntity
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
@Profile("database")
class MeterReadingReadRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun findByAccount(accountId: String, from: Instant?, to: Instant?): List<MeterReadingEntity> {
        val conditions = mutableListOf("account_id = :accountId")
        val parameters = mutableMapOf<String, Any?>("accountId" to accountId)
        from?.let { conditions += "reading_at >= :fromAt"; parameters["fromAt"] = it }
        to?.let { conditions += "reading_at <= :toAt"; parameters["toAt"] = it }
        return jdbc.query("SELECT * FROM meter_readings WHERE ${conditions.joinToString(" AND ")} ORDER BY reading_at ASC, id ASC", parameters) { rs, _ -> MeterReadingEntity(rs.getObject("id", UUID::class.java), rs.getString("account_id"), rs.getString("meter_id"), rs.getObject("reading_at", java.time.OffsetDateTime::class.java).toInstant(), rs.getBigDecimal("value"), rs.getBoolean("estimated_flag"), rs.getString("region")) }
    }

    fun findLatestForCaseAccount(accountId: String): MeterReadingEntity? = findByAccount(accountId, null, null).lastOrNull()
}
