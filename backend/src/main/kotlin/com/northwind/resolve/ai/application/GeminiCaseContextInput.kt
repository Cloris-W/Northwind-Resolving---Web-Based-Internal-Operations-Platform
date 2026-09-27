package com.northwind.resolve.ai.application

import java.math.BigDecimal
import java.time.Instant

data class GeminiCaseContextInput(val case: GeminiCaseFact, val timeline: List<GeminiTimelineFact>, val billing: GeminiBillingFact, val meter: GeminiMeterFact, val fieldVisits: List<GeminiFieldVisitFact>, val hasMeterReadings: Boolean, val hasFieldVisits: Boolean)
data class GeminiCaseFact(val category: String, val priority: String, val region: String, val status: String, val slaDays: Int, val openedAt: Instant)
data class GeminiTimelineFact(val eventType: String, val timestamp: Instant, val sourceSystem: String)
data class GeminiBillingFact(val latestException: GeminiBillingExceptionFact?, val correctionCount: Long)
data class GeminiBillingExceptionFact(val riskScore: Int, val riskLevel: String, val reasonCodes: List<String>, val status: String, val region: String)
data class GeminiMeterFact(val available: Boolean, val latest: GeminiMeterReadingFact?)
data class GeminiMeterReadingFact(val timestamp: Instant, val value: BigDecimal, val estimated: Boolean, val region: String)
data class GeminiFieldVisitFact(val status: String, val scheduledAt: Instant)
