package com.northwind.resolve.integration

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
import javax.sql.DataSource

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("database")
class DatabaseMigrationIntegrationTests {
    @Autowired
    private lateinit var dataSource: DataSource

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
    }
}
