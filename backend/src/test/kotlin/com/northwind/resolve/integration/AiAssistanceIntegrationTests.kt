package com.northwind.resolve.integration

import com.northwind.resolve.ai.application.GeminiCaseContextInput
import com.northwind.resolve.ai.application.GeminiProvider
import com.northwind.resolve.ai.application.GeminiRecommendationOutput
import com.northwind.resolve.ai.application.GeminiSummaryOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("database")
@Import(AiAssistanceIntegrationTests.GeminiDoubleConfiguration::class)
class AiAssistanceIntegrationTests {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var dataSource: DataSource

    @Test fun `valid summary and recommendation persist complete traceable analysis rows`() {
        mockMvc.perform(post("/api/ai/cases/NW-100001/summary")).andExpect(status().isOk)
        mockMvc.perform(post("/api/ai/cases/NW-100001/recommendation")).andExpect(status().isOk)
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT case_id, analysis_type, input_hash, model, output_json, created_at FROM ai_analysis WHERE case_id = ? ORDER BY analysis_type").use { statement ->
                statement.setString(1, "NW-100001"); statement.executeQuery().use { rows ->
                    assertTrue(rows.next()); assertEquals("NW-100001", rows.getString("case_id")); assertTrue(rows.getString("analysis_type") in setOf("SUMMARY", "RECOMMENDATION")); assertEquals(64, rows.getString("input_hash").length); assertTrue(rows.getString("model").isNotBlank()); assertTrue(rows.getString("output_json").isNotBlank()); assertTrue(rows.getTimestamp("created_at") != null)
                    assertTrue(rows.next()); assertEquals("NW-100001", rows.getString("case_id")); assertTrue(rows.getString("analysis_type") in setOf("SUMMARY", "RECOMMENDATION"))
                }
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class GeminiDoubleConfiguration {
        @Bean @Primary fun geminiProvider(): GeminiProvider = object : GeminiProvider {
            override fun summary(input: GeminiCaseContextInput) = GeminiSummaryOutput("test summary", "test classification", "test draft")
            override fun recommendation(input: GeminiCaseContextInput) = GeminiRecommendationOutput("test recommendation", "test rationale")
        }
    }

    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("northwind_ai_test").withUsername("test_user").withPassword("test_password")
        @DynamicPropertySource @JvmStatic fun databaseProperties(registry: DynamicPropertyRegistry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword) }
    }
}
