package com.northwind.resolve.integrations

import com.northwind.resolve.importing.BillingCorrectionIndicator
import com.northwind.resolve.importing.ImportedCaseReference
import com.northwind.resolve.importing.LegacySourceQuery
import com.northwind.resolve.importing.MeterRegionMetric
import com.northwind.resolve.importing.PersistedFieldVisitReference
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile
import java.time.LocalDate

interface CaseTrackProvider {
    fun findCase(caseId: String): ImportedCaseReference?
    fun findCasesByAccount(accountId: String): List<ImportedCaseReference>
}

interface MeterHubProvider {
    /** Challenge data supports regional monthly aggregates only; it has no account-level meter history. */
    fun findRegionalMetric(region: String, month: LocalDate): List<MeterRegionMetric>
}

interface BillingProvider {
    /** Returns source complaint correction indicators, not a billing history or correction workflow. */
    fun findCorrectionIndicators(accountId: String): List<BillingCorrectionIndicator>
}

interface FieldForceProvider {
    /** Returns only visits actually persisted by a later workflow. Phase 2 creates none. */
    fun findPersistedVisits(caseId: String): List<PersistedFieldVisitReference>
}

@Component
@Profile("database")
class CaseTrackMockAdapter(private val source: LegacySourceQuery) : CaseTrackProvider {
    override fun findCase(caseId: String) = source.findCase(caseId)
    override fun findCasesByAccount(accountId: String) = source.findCasesByAccount(accountId)
}

@Component
@Profile("database")
class MeterHubMockAdapter(private val source: LegacySourceQuery) : MeterHubProvider {
    override fun findRegionalMetric(region: String, month: LocalDate) = source.findMeterMetrics(region, month)
}

@Component
@Profile("database")
class BillingMockAdapter(private val source: LegacySourceQuery) : BillingProvider {
    override fun findCorrectionIndicators(accountId: String) = source.findBillingIndicators(accountId)
}

@Component
@Profile("database")
class FieldForceMockAdapter(private val source: LegacySourceQuery) : FieldForceProvider {
    override fun findPersistedVisits(caseId: String) = source.findFieldVisits(caseId)
}
