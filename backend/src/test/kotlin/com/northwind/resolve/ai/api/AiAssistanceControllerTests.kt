package com.northwind.resolve.ai.api

import com.northwind.resolve.ai.application.AiAssistanceService
import com.northwind.resolve.ai.application.AiUnavailableException
import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.common.api.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(AiAssistanceController::class)
@ActiveProfiles("database")
@Import(ApiExceptionHandler::class)
class AiAssistanceControllerTests {
    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var assistance: AiAssistanceService

    @Test fun `summary returns structured response`() {
        `when`(assistance.summary("test-case")).thenReturn(AiCaseSummaryDto("test-case", "summary", "classification", "draft", Instant.parse("2026-01-01T00:00:00Z"), "test-model"))
        mockMvc.perform(post("/api/ai/cases/test-case/summary")).andExpect(status().isOk).andExpect(jsonPath("$.summary").value("summary")).andExpect(jsonPath("$.customerResponseDraft").value("draft"))
    }

    @Test fun `recommendation returns structured response`() {
        `when`(assistance.recommendation("test-case")).thenReturn(AiCaseRecommendationDto("test-case", "recommendation", "rationale", Instant.parse("2026-01-01T00:00:00Z"), true, "test-model"))
        mockMvc.perform(post("/api/ai/cases/test-case/recommendation")).andExpect(status().isOk).andExpect(jsonPath("$.recommendation").value("recommendation")).andExpect(jsonPath("$.advisory").value(true))
    }

    @Test fun `unknown case has standard not found error`() {
        `when`(assistance.summary("missing")).thenThrow(CaseNotFoundException("missing"))
        mockMvc.perform(post("/api/ai/cases/missing/summary")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("CASE_NOT_FOUND")).andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test fun `provider unavailable has standard service unavailable error`() {
        `when`(assistance.summary("test-case")).thenThrow(AiUnavailableException())
        mockMvc.perform(post("/api/ai/cases/test-case/summary")).andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.status").value(503)).andExpect(jsonPath("$.code").value("AI_UNAVAILABLE")).andExpect(jsonPath("$.message").isNotEmpty).andExpect(jsonPath("$.traceId").isNotEmpty)
    }
}
