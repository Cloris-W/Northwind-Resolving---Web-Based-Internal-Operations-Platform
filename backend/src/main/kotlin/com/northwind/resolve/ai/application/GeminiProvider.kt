package com.northwind.resolve.ai.application

data class GeminiSummaryOutput(val summary: String, val classification: String, val customerResponseDraft: String)
data class GeminiRecommendationOutput(val recommendation: String, val rationale: String)

interface GeminiProvider {
    fun summary(input: GeminiCaseContextInput): GeminiSummaryOutput
    fun recommendation(input: GeminiCaseContextInput): GeminiRecommendationOutput
}
