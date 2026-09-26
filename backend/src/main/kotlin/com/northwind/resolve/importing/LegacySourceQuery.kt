package com.northwind.resolve.importing

import java.math.BigDecimal
import java.time.LocalDate

data class ImportedCaseReference(
    val caseId: String,
    val accountId: String,
    val sourceSystemId: String,
    val rawStatus: String,
)

data class MeterRegionMetric(
    val month: LocalDate,
    val region: String,
    val accounts: Int,
    val estimatedReadRate: BigDecimal,
    val smartMeterPenetration: BigDecimal,
    val billingExceptionsRaised: Int,
    val systemsServingRegion: String,
)

data class BillingCorrectionIndicator(
    val complaintId: String,
    val accountId: String,
    val billCorrectionValue: BigDecimal?,
)

data class PersistedFieldVisitReference(val visitId: String, val caseId: String)

interface LegacySourceQuery {
    fun findCase(caseId: String): ImportedCaseReference?
    fun findCasesByAccount(accountId: String): List<ImportedCaseReference>
    fun findMeterMetrics(region: String, month: LocalDate): List<MeterRegionMetric>
    fun findBillingIndicators(accountId: String): List<BillingCorrectionIndicator>
    fun findFieldVisits(caseId: String): List<PersistedFieldVisitReference>
}
