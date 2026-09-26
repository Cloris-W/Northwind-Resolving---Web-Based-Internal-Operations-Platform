package com.northwind.resolve.importing

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NorthwindCsvParserTests {
    private val parser = NorthwindCsvParser()
    private val data = Path.of("..", "data")

    @Test
    fun `parses each canonical CSV with its expected header and row count`() {
        assertEquals(15, parser.parse(data.resolve("northwind_systems.csv"), setOf("system_id", "system_name", "purpose", "year_installed", "vendor", "tech_stack", "records_held", "integration_method", "annual_run_cost", "owning_function", "notes")).size)
        assertEquals(25416, parser.parse(data.resolve("northwind_complaints.csv"), setOf("complaint_id", "date_opened", "date_closed", "status", "channel", "category", "priority", "region", "source_system", "transferred_between_systems", "sla_days", "days_to_close", "sla_breach", "reopened", "resolution_action", "resolvable_by_information_only", "bill_correction_value", "account_id")).size)
        assertEquals(24, parser.parse(data.resolve("northwind_monthly_kpis.csv"), setOf("month", "complaints_opened", "complaints_closed", "avg_days_to_close", "first_contact_resolution_rate", "inbound_calls", "cost_to_serve_per_account", "regulator_satisfaction_score_of_5")).size)
        assertEquals(144, parser.parse(data.resolve("northwind_meter_reads.csv"), setOf("month", "region", "accounts", "estimated_read_rate", "smart_meter_penetration", "billing_exceptions_raised", "systems_serving_region")).size)
        assertEquals(9, parser.parse(data.resolve("northwind_ai_pilot_2025.csv"), setOf("month", "assistant_sessions", "fully_contained_rate", "escalated_to_agent_rate", "abandoned_rate", "repeat_contact_within_7_days_rate", "assistant_csat_of_5", "complaint_raised_after_session_rate")).size)
        assertEquals(10, parser.parse(data.resolve("northwind_unit_costs.csv"), setOf("item", "unit_cost", "unit", "source_note")).size)
    }

    @Test
    fun `rejects an unexpected header`() {
        assertFailsWith<IllegalArgumentException> {
            parser.parse(data.resolve("northwind_systems.csv"), setOf("wrong_header"))
        }
    }
}
