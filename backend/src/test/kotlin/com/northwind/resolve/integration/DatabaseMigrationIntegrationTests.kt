package com.northwind.resolve.integration

import com.northwind.resolve.importing.LegacySourceRepository
import com.northwind.resolve.importing.NorthwindCsvImportService
import com.northwind.resolve.integrations.BillingProvider
import com.northwind.resolve.integrations.CaseTrackProvider
import com.northwind.resolve.integrations.FieldForceProvider
import com.northwind.resolve.integrations.MeterHubProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.time.LocalDate
import javax.sql.DataSource

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("database")
class DatabaseMigrationIntegrationTests {
    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var importService: NorthwindCsvImportService

    @Autowired
    private lateinit var sourceRepository: LegacySourceRepository

    @Autowired
    private lateinit var caseTrack: CaseTrackProvider

    @Autowired
    private lateinit var meterHub: MeterHubProvider

    @Autowired
    private lateinit var billing: BillingProvider

    @Autowired
    private lateinit var fieldForce: FieldForceProvider

    @Test
    fun `fresh PostgreSQL container receives all core operational tables`() {
        dataSource.connection.use { connection ->
            val tables = tableNames(connection)
            assertEquals(
                setOf(
                    "cases",
                    "case_events",
                    "meter_readings",
                    "billing_exceptions",
                    "bill_corrections",
                    "field_visits",
                    "ai_analysis",
                    "audit_events"
                ),
                tables.intersect(expectedTables)
            )
            assertTrue(tables.contains("flyway_schema_history"))
        }
    }

    @Test
    fun `fresh V1 plus V2 database imports all six files and repeated import is idempotent`() {
        dataSource.connection.use { connection ->
            assertEquals(15, count(connection, "legacy_systems"))
            assertEquals(25416, count(connection, "legacy_complaints"))
            assertEquals(25416, count(connection, "cases"))
            assertEquals(25416, count(connection, "case_events"))
            assertEquals(24, count(connection, "monthly_kpis"))
            assertEquals(144, count(connection, "meter_region_monthly_metrics"))
            assertEquals(9, count(connection, "ai_pilot_monthly_metrics"))
            assertEquals(10, count(connection, "unit_costs"))
        }

        importService.importAll()

        dataSource.connection.use { connection ->
            assertEquals(25416, count(connection, "cases"))
            assertEquals(25416, count(connection, "case_events"))
            assertEquals(25416, count(connection, "legacy_complaints"))
        }
    }

    @Test
    fun `source IDs are preserved and mock adapters expose only available challenge data`() {
        val imported = caseTrack.findCase("NW-100001")
        assertEquals("NW-100001", imported?.caseId)
        assertEquals("SYS-01", imported?.sourceSystemId)
        assertTrue(meterHub.findRegionalMetric("Ashford", LocalDate.of(2024, 10, 1)).isNotEmpty())
        assertTrue(billing.findCorrectionIndicators(imported!!.accountId).isNotEmpty())
        assertTrue(fieldForce.findPersistedVisits(imported.caseId).isEmpty())
    }

    companion object {
        private val expectedTables = setOf(
            "cases", "case_events", "meter_readings", "billing_exceptions",
            "bill_corrections", "field_visits", "ai_analysis", "audit_events"
        )

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("northwind_resolve_test")
            .withUsername("test_user")
            .withPassword("test_password")

        @DynamicPropertySource
        @JvmStatic
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }

        private fun tableNames(connection: Connection): Set<String> = connection.metaData
            .getTables(null, "public", "%", arrayOf("TABLE"))
            .use { resultSet ->
                buildSet {
                    while (resultSet.next()) add(resultSet.getString("TABLE_NAME"))
                }
            }

        private fun count(connection: Connection, table: String): Int = connection
            .createStatement()
            .use { statement -> statement.executeQuery("SELECT COUNT(*) FROM $table").use { result -> result.next(); result.getInt(1) } }
    }
}
