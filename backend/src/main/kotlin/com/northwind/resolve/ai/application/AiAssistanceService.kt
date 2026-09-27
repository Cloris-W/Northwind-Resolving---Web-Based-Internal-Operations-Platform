package com.northwind.resolve.ai.application

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.northwind.resolve.accounts.application.AccountContextService
import com.northwind.resolve.ai.api.AiCaseRecommendationDto
import com.northwind.resolve.ai.api.AiCaseSummaryDto
import com.northwind.resolve.ai.config.GeminiProperties
import com.northwind.resolve.ai.domain.AiAnalysisEntity
import com.northwind.resolve.ai.domain.AiAnalysisType
import com.northwind.resolve.ai.persistence.AiAnalysisRepository
import com.northwind.resolve.billing.application.BillingAiContextService
import com.northwind.resolve.cases.application.CaseWorkspaceService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant

@Service
@Profile("database")
class AiAssistanceService(private val cases: CaseWorkspaceService, private val accounts: AccountContextService, private val billing: BillingAiContextService, private val provider: GeminiProvider, private val analyses: AiAnalysisRepository, private val properties: GeminiProperties, private val mapper: ObjectMapper) {
    fun summary(caseId: String): AiCaseSummaryDto {
        val input = input(caseId); val output = provider.summary(input).valid(); val now = Instant.now()
        persist(caseId, AiAnalysisType.SUMMARY, input, output, now)
        return AiCaseSummaryDto(caseId, output.summary, output.classification, output.customerResponseDraft, now, properties.model)
    }

    fun recommendation(caseId: String): AiCaseRecommendationDto {
        val input = input(caseId); val output = provider.recommendation(input).valid(); val now = Instant.now()
        persist(caseId, AiAnalysisType.RECOMMENDATION, input, output, now)
        return AiCaseRecommendationDto(caseId, output.recommendation, output.rationale, now, true, properties.model)
    }

    private fun input(caseId: String): GeminiCaseContextInput {
        val context = cases.context(caseId)
        val readings = accounts.meterHistory(context.case.accountId, null, null).readings.sortedBy { it.timestamp }
        val latest = readings.lastOrNull(); val billingContext = billing.forCase(caseId, context.case.accountId)
        return GeminiCaseContextInput(
            GeminiCaseFact(context.case.category.name, context.case.priority.name, context.case.region, context.case.status.name, context.case.slaDays, context.case.openedAt),
            cases.timeline(caseId).events.sortedWith(compareBy({ it.timestamp }, { it.eventId.toString() })).map { GeminiTimelineFact(it.eventType.name, it.timestamp, it.sourceSystem.name) },
            GeminiBillingFact(billingContext.latestException?.let { exception -> GeminiBillingExceptionFact(exception.riskScore, exception.riskLevel.name, mapper.readValue(exception.reasonCodes, List::class.java).map { it.toString() }, exception.status.name, exception.region) }, billingContext.correctionCount),
            GeminiMeterFact(latest != null, latest?.let { GeminiMeterReadingFact(it.timestamp, it.value, it.estimated, it.region) }),
            context.fieldVisits.sortedBy { it.scheduledAt }.map { GeminiFieldVisitFact(it.status.name, it.scheduledAt) }, readings.isNotEmpty(), context.fieldVisits.isNotEmpty(),
        )
    }

    private fun persist(caseId: String, type: AiAnalysisType, input: GeminiCaseContextInput, output: Any, now: Instant) {
        analyses.insert(AiAnalysisEntity(caseId = caseId, analysisType = type, inputHash = hash(type, input), model = properties.model, outputJson = mapper.writeValueAsString(output), createdAt = now))
    }

    internal fun hash(type: AiAnalysisType, input: GeminiCaseContextInput): String {
        val stable = mapper.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        val bytes = stable.writeValueAsBytes(mapOf("analysisType" to type.name, "input" to input))
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

private fun GeminiSummaryOutput.valid() = GeminiSummaryOutput(summary.checked("summary", 4000), classification.checked("classification", 100), customerResponseDraft.checked("customerResponseDraft", 4000))
private fun GeminiRecommendationOutput.valid() = GeminiRecommendationOutput(recommendation.checked("recommendation", 2000), rationale.checked("rationale", 4000))
private fun String.checked(name: String, max: Int): String = trim().also { if (it.isEmpty() || it.length > max) throw AiUnavailableException("AI assistance returned invalid $name") }
