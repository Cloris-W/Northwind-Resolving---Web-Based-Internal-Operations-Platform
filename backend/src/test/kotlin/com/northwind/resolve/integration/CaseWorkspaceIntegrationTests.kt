package com.northwind.resolve.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("database")
class CaseWorkspaceIntegrationTests {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test fun `real imported case can be searched with chronological timeline and truthful empty source-limited sections`() {
        mockMvc.perform(get("/api/cases").param("accountId", "ACC-943644")).andExpect(status().isOk).andExpect(jsonPath("$.items[?(@.caseId == 'NW-100001')]").isNotEmpty)
        mockMvc.perform(get("/api/cases/NW-100001")).andExpect(status().isOk).andExpect(jsonPath("$.case.caseId").value("NW-100001")).andExpect(jsonPath("$.fieldVisits").isEmpty).andExpect(jsonPath("$.latestMeterReading").doesNotExist())
        val timeline = mockMvc.perform(get("/api/cases/NW-100001/timeline")).andExpect(status().isOk).andReturn().response.contentAsString
        val timestamps = objectMapper.readTree(timeline).path("events").map { it.path("timestamp").asText() }
        assertEquals(timestamps.sorted(), timestamps)
        assertTrue(timestamps.isNotEmpty())
        mockMvc.perform(get("/api/accounts/ACC-943644/meter-readings")).andExpect(status().isOk).andExpect(jsonPath("$.readings").isEmpty)
        mockMvc.perform(get("/api/accounts/ACC-943644/billing")).andExpect(status().isOk).andExpect(jsonPath("$.bills").isEmpty).andExpect(jsonPath("$.corrections").isEmpty)
    }

    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("northwind_workspace_test").withUsername("test_user").withPassword("test_password")
        @DynamicPropertySource @JvmStatic fun databaseProperties(registry: DynamicPropertyRegistry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword) }
    }
}
