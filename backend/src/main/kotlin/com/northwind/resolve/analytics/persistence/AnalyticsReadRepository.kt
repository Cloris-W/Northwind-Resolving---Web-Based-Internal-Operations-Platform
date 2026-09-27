package com.northwind.resolve.analytics.persistence

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

data class MonthlyKpiRow(val month: LocalDate, val opened: Int, val closed: Int, val fcr: BigDecimal?)
data class MeterMetricRow(val month: LocalDate, val accounts: Int, val rate: BigDecimal?)

@Repository @Profile("database")
class AnalyticsReadRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun latestMonths() = jdbc.query("SELECT month FROM monthly_kpis ORDER BY month DESC LIMIT 12", emptyMap<String, Any>()) { rs, _ -> rs.getObject(1, LocalDate::class.java) }.sorted()
    fun monthlyKpis(from: LocalDate, to: LocalDate) = jdbc.query("SELECT month, complaints_opened, complaints_closed, first_contact_resolution_rate FROM monthly_kpis WHERE month BETWEEN :from AND :to ORDER BY month", mapOf("from" to from, "to" to to)) { rs, _ -> MonthlyKpiRow(rs.getObject(1, LocalDate::class.java), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)) }
    fun backlog(region: String?) = jdbc.queryForObject("SELECT COUNT(*) FROM cases WHERE status NOT IN ('CLOSED','RESOLVED')" + regionClause(region), params(region), Int::class.java) ?: 0
    fun complaintCounts(from: LocalDate, to: LocalDate, region: String?): Triple<Int, Int, Int> = jdbc.queryForObject("SELECT COUNT(*), COUNT(*) FILTER (WHERE sla_breach), COUNT(*) FILTER (WHERE transferred_between_systems) FROM legacy_complaints WHERE date_opened BETWEEN :from AND :to" + regionClause(region), params(region) + mapOf("from" to from, "to" to to)) { rs, _ -> Triple(rs.getInt(1), rs.getInt(2), rs.getInt(3)) } ?: Triple(0, 0, 0)
    fun reopenedCount(from: LocalDate, to: LocalDate, region: String?) = jdbc.queryForObject("SELECT COUNT(*) FROM legacy_complaints WHERE date_opened BETWEEN :from AND :to AND reopened" + regionClause(region), params(region) + mapOf("from" to from, "to" to to), Int::class.java) ?: 0
    fun billingExceptions(from: LocalDate, to: LocalDate, region: String?) = jdbc.queryForObject("SELECT COUNT(*) FROM billing_exceptions WHERE created_at >= :from AND created_at < :to" + regionClause(region), params(region) + mapOf("from" to from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), "to" to to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()), Int::class.java) ?: 0
    fun meterRows(from: LocalDate, to: LocalDate, region: String?) = jdbc.query("SELECT month, accounts, estimated_read_rate FROM meter_region_monthly_metrics WHERE month BETWEEN :from AND :to" + regionClause(region) + " ORDER BY month", params(region) + mapOf("from" to from, "to" to to)) { rs, _ -> MeterMetricRow(rs.getObject(1, LocalDate::class.java), rs.getInt(2), rs.getBigDecimal(3)) }
    fun unitCost(item: String) = jdbc.query("SELECT unit_cost FROM unit_costs WHERE item=:item", mapOf("item" to item)) { rs, _ -> rs.getBigDecimal(1) }.firstOrNull()
    fun correctionCount(from: LocalDate, to: LocalDate) = jdbc.queryForObject("SELECT COUNT(*) FROM legacy_complaints WHERE date_opened BETWEEN :from AND :to AND bill_correction_value IS NOT NULL", mapOf("from" to from, "to" to to), Int::class.java) ?: 0
    private fun regionClause(region: String?) = if (region.isNullOrBlank()) "" else " AND region=:region"
    private fun params(region: String?) = if (region.isNullOrBlank()) emptyMap() else mapOf("region" to region)
}
