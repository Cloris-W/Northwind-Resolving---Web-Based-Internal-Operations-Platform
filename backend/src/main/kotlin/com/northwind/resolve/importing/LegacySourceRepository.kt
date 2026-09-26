package com.northwind.resolve.importing

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.sql.Types
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Repository
@Profile("database")
class LegacySourceRepository(private val jdbc: NamedParameterJdbcTemplate) : LegacySourceQuery {
    fun upsertSystem(row: Map<String, String>) = update(
        """INSERT INTO legacy_systems (system_id, system_name, purpose, year_installed, vendor, tech_stack, records_held, integration_method, annual_run_cost, owning_function, notes)
           VALUES (:system_id, :system_name, :purpose, :year_installed, :vendor, :tech_stack, :records_held, :integration_method, :annual_run_cost, :owning_function, :notes)
           ON CONFLICT (system_id) DO UPDATE SET system_name = EXCLUDED.system_name, purpose = EXCLUDED.purpose, year_installed = EXCLUDED.year_installed, vendor = EXCLUDED.vendor, tech_stack = EXCLUDED.tech_stack, records_held = EXCLUDED.records_held, integration_method = EXCLUDED.integration_method, annual_run_cost = EXCLUDED.annual_run_cost, owning_function = EXCLUDED.owning_function, notes = EXCLUDED.notes""",
        row + mapOf("year_installed" to row.int("year_installed"), "annual_run_cost" to row.decimal("annual_run_cost")),
    )

    fun upsertComplaint(row: Map<String, String>) = update(
        """INSERT INTO legacy_complaints (complaint_id, date_opened, date_closed, raw_status, channel, raw_category, raw_priority, region, source_system_id, transferred_between_systems, sla_days, days_to_close, sla_breach, reopened, resolution_action, resolvable_by_information_only, bill_correction_value, account_id)
           VALUES (:complaint_id, :date_opened, :date_closed, :status, :channel, :category, :priority, :region, :source_system, :transferred_between_systems, :sla_days, :days_to_close, :sla_breach, :reopened, :resolution_action, :resolvable_by_information_only, :bill_correction_value, :account_id)
           ON CONFLICT (complaint_id) DO UPDATE SET date_opened = EXCLUDED.date_opened, date_closed = EXCLUDED.date_closed, raw_status = EXCLUDED.raw_status, channel = EXCLUDED.channel, raw_category = EXCLUDED.raw_category, raw_priority = EXCLUDED.raw_priority, region = EXCLUDED.region, source_system_id = EXCLUDED.source_system_id, transferred_between_systems = EXCLUDED.transferred_between_systems, sla_days = EXCLUDED.sla_days, days_to_close = EXCLUDED.days_to_close, sla_breach = EXCLUDED.sla_breach, reopened = EXCLUDED.reopened, resolution_action = EXCLUDED.resolution_action, resolvable_by_information_only = EXCLUDED.resolvable_by_information_only, bill_correction_value = EXCLUDED.bill_correction_value, account_id = EXCLUDED.account_id""",
        row + mapOf(
            "date_opened" to row.date("date_opened"), "date_closed" to row.optionalDate("date_closed"),
            "transferred_between_systems" to row.bool("transferred_between_systems"), "sla_days" to row.int("sla_days"),
            "days_to_close" to row.optionalInt("days_to_close"), "sla_breach" to row.bool("sla_breach"), "reopened" to row.bool("reopened"),
            "resolvable_by_information_only" to row.optionalBoolean("resolvable_by_information_only"), "bill_correction_value" to row.optionalDecimal("bill_correction_value"),
        ),
    )

    fun upsertCase(row: Map<String, String>) = updateCase(
        """INSERT INTO cases (case_id, account_id, source_system_id, category, priority, region, status, sla_days, opened_at, closed_at, assigned_team)
           VALUES (:complaint_id, :account_id, :source_system, :category_normalized, :priority_normalized, :region, :status_normalized, :sla_days, :opened_at, :closed_at, 'UNASSIGNED_IMPORT')
           ON CONFLICT (case_id) DO UPDATE SET account_id = EXCLUDED.account_id, source_system_id = EXCLUDED.source_system_id, category = EXCLUDED.category, priority = EXCLUDED.priority, region = EXCLUDED.region, status = EXCLUDED.status, sla_days = EXCLUDED.sla_days, opened_at = EXCLUDED.opened_at, closed_at = EXCLUDED.closed_at""",
        row + mapOf(
            "category_normalized" to ComplaintNormalization.category(row.getValue("category")).name,
            "priority_normalized" to ComplaintNormalization.priority(row.getValue("priority")).name,
            "status_normalized" to ComplaintNormalization.status(row.getValue("status")).name,
            "opened_at" to row.date("date_opened").atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
            "closed_at" to row.optionalDate("date_closed")?.atStartOfDay(ZoneOffset.UTC)?.toOffsetDateTime(),
            "sla_days" to row.int("sla_days"),
        ),
    )

    fun insertComplaintCreatedEvent(row: Map<String, String>) = update(
        """INSERT INTO case_events (event_id, case_id, event_type, source_system, occurred_at, actor, description, metadata_json)
           VALUES (:event_id, :complaint_id, 'COMPLAINT_CREATED', 'CASETRACK', :occurred_at, 'CSV_IMPORT', 'Imported complaint record', CAST(:metadata_json AS jsonb))
           ON CONFLICT (event_id) DO NOTHING""",
        mapOf(
            "event_id" to UUID.nameUUIDFromBytes("northwind-complaint-created:${row.getValue("complaint_id")}".toByteArray()),
            "complaint_id" to row.getValue("complaint_id"),
            "occurred_at" to row.date("date_opened").atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
            "metadata_json" to "{\"sourceSystemId\":\"${row.getValue("source_system")}\",\"importedFrom\":\"northwind_complaints.csv\"}",
        ),
    )

    fun upsertMonthlyKpi(row: Map<String, String>) = update(
        """INSERT INTO monthly_kpis (month, complaints_opened, complaints_closed, avg_days_to_close, first_contact_resolution_rate, inbound_calls, cost_to_serve_per_account, regulator_satisfaction_score_of_5)
           VALUES (:month, :complaints_opened, :complaints_closed, :avg_days_to_close, :first_contact_resolution_rate, :inbound_calls, :cost_to_serve_per_account, :regulator_satisfaction_score_of_5)
           ON CONFLICT (month) DO UPDATE SET complaints_opened = EXCLUDED.complaints_opened, complaints_closed = EXCLUDED.complaints_closed, avg_days_to_close = EXCLUDED.avg_days_to_close, first_contact_resolution_rate = EXCLUDED.first_contact_resolution_rate, inbound_calls = EXCLUDED.inbound_calls, cost_to_serve_per_account = EXCLUDED.cost_to_serve_per_account, regulator_satisfaction_score_of_5 = EXCLUDED.regulator_satisfaction_score_of_5""",
        row.numericColumns("month", setOf("complaints_opened", "complaints_closed", "inbound_calls")),
    )

    fun upsertMeterMetric(row: Map<String, String>) = update(
        """INSERT INTO meter_region_monthly_metrics (month, region, accounts, estimated_read_rate, smart_meter_penetration, billing_exceptions_raised, systems_serving_region)
           VALUES (:month, :region, :accounts, :estimated_read_rate, :smart_meter_penetration, :billing_exceptions_raised, :systems_serving_region)
           ON CONFLICT (month, region) DO UPDATE SET accounts = EXCLUDED.accounts, estimated_read_rate = EXCLUDED.estimated_read_rate, smart_meter_penetration = EXCLUDED.smart_meter_penetration, billing_exceptions_raised = EXCLUDED.billing_exceptions_raised, systems_serving_region = EXCLUDED.systems_serving_region""",
        row.numericColumns("month", setOf("accounts", "billing_exceptions_raised")),
    )

    fun upsertAiMetric(row: Map<String, String>) = update(
        """INSERT INTO ai_pilot_monthly_metrics (month, assistant_sessions, fully_contained_rate, escalated_to_agent_rate, abandoned_rate, repeat_contact_within_7_days_rate, assistant_csat_of_5, complaint_raised_after_session_rate)
           VALUES (:month, :assistant_sessions, :fully_contained_rate, :escalated_to_agent_rate, :abandoned_rate, :repeat_contact_within_7_days_rate, :assistant_csat_of_5, :complaint_raised_after_session_rate)
           ON CONFLICT (month) DO UPDATE SET assistant_sessions = EXCLUDED.assistant_sessions, fully_contained_rate = EXCLUDED.fully_contained_rate, escalated_to_agent_rate = EXCLUDED.escalated_to_agent_rate, abandoned_rate = EXCLUDED.abandoned_rate, repeat_contact_within_7_days_rate = EXCLUDED.repeat_contact_within_7_days_rate, assistant_csat_of_5 = EXCLUDED.assistant_csat_of_5, complaint_raised_after_session_rate = EXCLUDED.complaint_raised_after_session_rate""",
        row.numericColumns("month", setOf("assistant_sessions")),
    )

    fun upsertUnitCost(row: Map<String, String>) = update(
        """INSERT INTO unit_costs (item, unit_cost, unit, source_note) VALUES (:item, :unit_cost, :unit, :source_note)
           ON CONFLICT (item) DO UPDATE SET unit_cost = EXCLUDED.unit_cost, unit = EXCLUDED.unit, source_note = EXCLUDED.source_note""",
        row + mapOf("unit_cost" to row.decimal("unit_cost")),
    )

    override fun findCase(caseId: String): ImportedCaseReference? = jdbc.query(
        "SELECT cases.case_id, cases.account_id, legacy_complaints.source_system_id, raw_status FROM cases JOIN legacy_complaints ON complaint_id = cases.case_id WHERE cases.case_id = :caseId",
        mapOf("caseId" to caseId),
    ) { rs, _ -> ImportedCaseReference(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) }.firstOrNull()

    override fun findCasesByAccount(accountId: String): List<ImportedCaseReference> = jdbc.query(
        "SELECT cases.case_id, cases.account_id, legacy_complaints.source_system_id, raw_status FROM cases JOIN legacy_complaints ON complaint_id = cases.case_id WHERE cases.account_id = :accountId ORDER BY cases.case_id",
        mapOf("accountId" to accountId),
    ) { rs, _ -> ImportedCaseReference(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) }

    override fun findMeterMetrics(region: String, month: LocalDate): List<MeterRegionMetric> = jdbc.query(
        "SELECT month, region, accounts, estimated_read_rate, smart_meter_penetration, billing_exceptions_raised, systems_serving_region FROM meter_region_monthly_metrics WHERE region = :region AND month = :month",
        mapOf("region" to region, "month" to month),
    ) { rs, _ -> MeterRegionMetric(rs.getObject(1, LocalDate::class.java), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getInt(6), rs.getString(7)) }

    override fun findBillingIndicators(accountId: String): List<BillingCorrectionIndicator> = jdbc.query(
        "SELECT complaint_id, account_id, bill_correction_value FROM legacy_complaints WHERE account_id = :accountId ORDER BY complaint_id",
        mapOf("accountId" to accountId),
    ) { rs, _ -> BillingCorrectionIndicator(rs.getString(1), rs.getString(2), rs.getBigDecimal(3)) }

    override fun findFieldVisits(caseId: String): List<PersistedFieldVisitReference> = jdbc.query(
        "SELECT id AS visit_id, case_id, status, scheduled_at, completed_at, outcome FROM field_visits WHERE case_id = :caseId ORDER BY scheduled_at ASC, id ASC",
        mapOf("caseId" to caseId),
    ) { rs, _ -> PersistedFieldVisitReference(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getString(3), rs.getObject(4, java.time.OffsetDateTime::class.java).toInstant(), rs.getObject(5, java.time.OffsetDateTime::class.java)?.toInstant(), rs.getString(6)) }

    private fun update(sql: String, params: Map<String, Any?>) = jdbc.update(sql, MapSqlParameterSource(params))

    private fun updateCase(sql: String, params: Map<String, Any?>) = jdbc.update(
        sql,
        MapSqlParameterSource(params).apply {
            addValue("opened_at", params.getValue("opened_at"), Types.TIMESTAMP_WITH_TIMEZONE)
            addValue("closed_at", params["closed_at"], Types.TIMESTAMP_WITH_TIMEZONE)
        },
    )
}

private fun Map<String, String>.date(key: String) = LocalDate.parse(getValue(key))
private fun Map<String, String>.optionalDate(key: String) = getValue(key).takeIf { it.isNotBlank() }?.let(LocalDate::parse)
private fun Map<String, String>.int(key: String) = getValue(key).toInt()
private fun Map<String, String>.optionalInt(key: String) = getValue(key).takeIf { it.isNotBlank() }?.toInt()
private fun Map<String, String>.decimal(key: String) = getValue(key).toBigDecimal()
private fun Map<String, String>.optionalDecimal(key: String) = getValue(key).takeIf { it.isNotBlank() }?.toBigDecimal()
private fun Map<String, String>.bool(key: String) = when (getValue(key)) {
    "1", "true" -> true
    "0", "false" -> false
    else -> error("Unsupported boolean value for $key: ${getValue(key)}")
}
private fun Map<String, String>.optionalBoolean(key: String) = getValue(key)
    .takeIf { it.isNotBlank() }
    ?.let { value -> when (value) {
        "1", "true" -> true
        "0", "false" -> false
        else -> error("Unsupported boolean value for $key: $value")
    } }
private fun Map<String, String>.numericColumns(dateColumn: String, integerColumns: Set<String>): Map<String, Any?> = mapValues { (key, value) ->
    when {
        key == dateColumn -> parseSourceMonth(value)
        key in integerColumns -> value.toInt()
        value.matches(Regex("-?\\d+(\\.\\d+)?")) -> value.toBigDecimal()
        else -> value
    }
}

private fun parseSourceMonth(value: String): LocalDate = when {
    value.matches(Regex("\\d{4}-\\d{2}")) -> YearMonth.parse(value).atDay(1)
    value.matches(Regex("[A-Za-z]{3}-\\d{2}")) -> YearMonth.parse(value, DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH)).atDay(1)
    else -> LocalDate.parse(value)
}
