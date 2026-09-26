package com.northwind.resolve.importing

import org.springframework.stereotype.Service
import org.springframework.context.annotation.Profile
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Path

data class ImportResult(val sourceFile: String, val rowsRead: Int)

@Service
@Profile("database")
class NorthwindCsvImportService(
    private val repository: LegacySourceRepository,
    private val transactionTemplate: TransactionTemplate,
    private val properties: NorthwindDataProperties,
    private val parser: NorthwindCsvParser = NorthwindCsvParser(),
) {
    fun importAll(): List<ImportResult> = listOf(
        importFile("northwind_systems.csv", SYSTEM_HEADERS) { repository.upsertSystem(it) },
        importFile("northwind_complaints.csv", COMPLAINT_HEADERS) {
            repository.upsertComplaint(it)
            repository.upsertCase(it)
            repository.insertComplaintCreatedEvent(it)
        },
        importFile("northwind_monthly_kpis.csv", KPI_HEADERS) { repository.upsertMonthlyKpi(it) },
        importFile("northwind_meter_reads.csv", METER_HEADERS) { repository.upsertMeterMetric(it) },
        importFile("northwind_ai_pilot_2025.csv", AI_HEADERS) { repository.upsertAiMetric(it) },
        importFile("northwind_unit_costs.csv", UNIT_COST_HEADERS) { repository.upsertUnitCost(it) },
    )

    private fun importFile(fileName: String, headers: Set<String>, write: (Map<String, String>) -> Unit): ImportResult {
        // Parsing and header validation complete before a transaction is opened; a write failure rolls back this whole source.
        val rows = parser.parse(Path.of(properties.dataDirectory).resolve(fileName), headers)
        transactionTemplate.executeWithoutResult { rows.forEach(write) }
        return ImportResult(fileName, rows.size)
    }

    private companion object {
        val SYSTEM_HEADERS = setOf("system_id", "system_name", "purpose", "year_installed", "vendor", "tech_stack", "records_held", "integration_method", "annual_run_cost", "owning_function", "notes")
        val COMPLAINT_HEADERS = setOf("complaint_id", "date_opened", "date_closed", "status", "channel", "category", "priority", "region", "source_system", "transferred_between_systems", "sla_days", "days_to_close", "sla_breach", "reopened", "resolution_action", "resolvable_by_information_only", "bill_correction_value", "account_id")
        val KPI_HEADERS = setOf("month", "complaints_opened", "complaints_closed", "avg_days_to_close", "first_contact_resolution_rate", "inbound_calls", "cost_to_serve_per_account", "regulator_satisfaction_score_of_5")
        val METER_HEADERS = setOf("month", "region", "accounts", "estimated_read_rate", "smart_meter_penetration", "billing_exceptions_raised", "systems_serving_region")
        val AI_HEADERS = setOf("month", "assistant_sessions", "fully_contained_rate", "escalated_to_agent_rate", "abandoned_rate", "repeat_contact_within_7_days_rate", "assistant_csat_of_5", "complaint_raised_after_session_rate")
        val UNIT_COST_HEADERS = setOf("item", "unit_cost", "unit", "source_note")
    }
}
