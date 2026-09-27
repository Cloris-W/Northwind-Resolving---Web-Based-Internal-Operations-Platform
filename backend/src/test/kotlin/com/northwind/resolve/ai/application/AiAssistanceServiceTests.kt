package com.northwind.resolve.ai.application

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.northwind.resolve.accounts.application.AccountContextService
import com.northwind.resolve.ai.config.GeminiProperties
import com.northwind.resolve.ai.domain.AiAnalysisEntity
import com.northwind.resolve.ai.domain.AiAnalysisType
import com.northwind.resolve.ai.persistence.AiAnalysisRepository
import com.northwind.resolve.billing.application.BillingAiContext
import com.northwind.resolve.billing.application.BillingAiContextService
import com.northwind.resolve.cases.api.CaseContextResponse
import com.northwind.resolve.cases.api.CaseDto
import com.northwind.resolve.cases.api.CaseTimelineResponse
import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.metering.api.MeterReadingsResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

class AiAssistanceServiceTests {
    private val cases = mock(CaseWorkspaceService::class.java); private val accounts = mock(AccountContextService::class.java); private val billing = mock(BillingAiContextService::class.java); private val repository = mock(AiAnalysisRepository::class.java); private val provider = StubProvider()
    private val service = AiAssistanceService(cases, accounts, billing, provider, repository, GeminiProperties(model = "test-model"), jacksonObjectMapper().registerModule(JavaTimeModule()))
    private fun arrange() { val c = CaseDto("test-case", "test-account", CaseCategory.BILLING, CasePriority.HIGH, "Test", CaseStatus.OPEN, 10, Instant.parse("2026-01-01T00:00:00Z"), null, "Team"); `when`(cases.context("test-case")).thenReturn(CaseContextResponse(c, null, null, emptyList())); `when`(cases.timeline("test-case")).thenReturn(CaseTimelineResponse("test-case", emptyList())); `when`(accounts.meterHistory("test-account", null, null)).thenReturn(MeterReadingsResponse("test-account", emptyList())); `when`(billing.forCase("test-case", "test-account")).thenReturn(BillingAiContext(null, 0)) }
    @Test fun `valid summary persists approved fields`() { arrange(); val result = service.summary("test-case"); assertEquals("summary", result.summary); assertEquals("classification", result.classification); assertEquals("draft", result.customerResponseDraft); val stored = inserts().single(); assertEquals(AiAnalysisType.SUMMARY, stored.analysisType); assertEquals("test-model", stored.model); assertEquals("test-case", stored.caseId) }
    @Test fun `valid recommendation persists recommendation analysis`() { arrange(); val result = service.recommendation("test-case"); assertEquals("recommendation", result.recommendation); assertTrue(result.advisory); assertEquals(AiAnalysisType.RECOMMENDATION, inserts().single().analysisType) }
    @Test fun `invalid provider output is not persisted`() { arrange(); provider.summaryOutput = GeminiSummaryOutput(" ", "classification", "draft"); assertThrows(AiUnavailableException::class.java) { service.summary("test-case") }; verifyNoInteractions(repository) }
    @Test fun `missing required output is not persisted`() { arrange(); provider.summaryOutput = GeminiSummaryOutput("summary", "", "draft"); assertThrows(AiUnavailableException::class.java) { service.summary("test-case") }; verifyNoInteractions(repository) }
    @Test fun `over-length summary is not persisted`() { arrange(); provider.summaryOutput = GeminiSummaryOutput("x".repeat(4001), "classification", "draft"); assertThrows(AiUnavailableException::class.java) { service.summary("test-case") }; verifyNoInteractions(repository) }
    @Test fun `over-length classification is not persisted`() { arrange(); provider.summaryOutput = GeminiSummaryOutput("summary", "x".repeat(101), "draft"); assertThrows(AiUnavailableException::class.java) { service.summary("test-case") }; verifyNoInteractions(repository) }
    @Test fun `over-length customer draft is not persisted`() { arrange(); provider.summaryOutput = GeminiSummaryOutput("summary", "classification", "x".repeat(4001)); assertThrows(AiUnavailableException::class.java) { service.summary("test-case") }; verifyNoInteractions(repository) }
    @Test fun `over-length recommendation and rationale are not persisted`() { arrange(); provider.recommendationOutput = GeminiRecommendationOutput("x".repeat(2001), "rationale"); assertThrows(AiUnavailableException::class.java) { service.recommendation("test-case") }; provider.recommendationOutput = GeminiRecommendationOutput("recommendation", "x".repeat(4001)); assertThrows(AiUnavailableException::class.java) { service.recommendation("test-case") }; verifyNoInteractions(repository) }
    @Test fun `unknown case is propagated`() { `when`(cases.context("missing")).thenThrow(CaseNotFoundException("missing")); assertThrows(CaseNotFoundException::class.java) { service.summary("missing") }; assertEquals(0, provider.summaryCalls) }
    @Test fun `repeated request persists fresh rows with deterministic hash`() { arrange(); service.summary("test-case"); service.summary("test-case"); val stored = inserts(); assertEquals(2, stored.map { it.id }.distinct().size); assertEquals(stored[0].inputHash, stored[1].inputHash) }
    private fun inserts() = mockingDetails(repository).invocations.filter { it.method.name == "insert" }.map { it.arguments.single() as AiAnalysisEntity }
    private class StubProvider : GeminiProvider { var summaryOutput = GeminiSummaryOutput("summary", "classification", "draft"); var recommendationOutput = GeminiRecommendationOutput("recommendation", "rationale"); var summaryCalls = 0; override fun summary(input: GeminiCaseContextInput): GeminiSummaryOutput { summaryCalls++; return summaryOutput }; override fun recommendation(input: GeminiCaseContextInput) = recommendationOutput }
}
