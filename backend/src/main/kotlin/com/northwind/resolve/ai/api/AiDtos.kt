package com.northwind.resolve.ai.api

import java.time.Instant

data class AiCaseSummaryDto(val caseId: String, val summary: String, val classification: String, val generatedAt: Instant, val model: String? = null)
data class AiCaseRecommendationDto(val caseId: String, val recommendation: String, val rationale: String, val generatedAt: Instant, val advisory: Boolean = true, val model: String? = null)
