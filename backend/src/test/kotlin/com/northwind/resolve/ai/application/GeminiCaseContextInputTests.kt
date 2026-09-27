package com.northwind.resolve.ai.application

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.northwind.resolve.ai.config.GeminiProperties
import com.northwind.resolve.ai.domain.AiAnalysisType
import com.northwind.resolve.ai.persistence.AiAnalysisRepository
import com.northwind.resolve.accounts.application.AccountContextService
import com.northwind.resolve.billing.application.BillingAiContextService
import com.northwind.resolve.cases.application.CaseWorkspaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant

class GeminiCaseContextInputTests {
    @Test
    fun `input hash is deterministic for the same canonical input`() {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val service = AiAssistanceService(mock(CaseWorkspaceService::class.java), mock(AccountContextService::class.java), mock(BillingAiContextService::class.java), mock(GeminiProvider::class.java), mock(AiAnalysisRepository::class.java), GeminiProperties(), mapper)
        val input = GeminiCaseContextInput(GeminiCaseFact("BILLING", "HIGH", "Test", "OPEN", 10, Instant.parse("2026-01-01T00:00:00Z")), emptyList(), GeminiBillingFact(null, 0), GeminiMeterFact(false, null), emptyList(), false, false)
        assertEquals(service.hash(AiAnalysisType.SUMMARY, input), service.hash(AiAnalysisType.SUMMARY, input))
    }
}
