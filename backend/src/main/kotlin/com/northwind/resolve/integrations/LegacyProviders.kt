package com.northwind.resolve.integrations

import com.northwind.resolve.importing.BillingCorrectionIndicator
import com.northwind.resolve.importing.ImportedCaseReference
import com.northwind.resolve.importing.LegacySourceQuery
import com.northwind.resolve.importing.MeterRegionMetric
import com.northwind.resolve.importing.PersistedFieldVisitReference
import com.northwind.resolve.fieldforce.domain.FieldVisitEntity
import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import com.northwind.resolve.fieldforce.persistence.FieldVisitRepository
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

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
    fun requestVisit(context: FieldForceRequestContext, visitId: UUID): PersistedFieldVisitReference
}

data class FieldForceRequestContext(val caseId: String, val region: String, val requestedFor: Instant, val visitReason: String, val meterId: String?, val instructions: String?)

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
class FieldForceMockAdapter(private val source: LegacySourceQuery, private val visits: FieldVisitRepository? = null) : FieldForceProvider {
    override fun findPersistedVisits(caseId: String) = source.findFieldVisits(caseId)
    override fun requestVisit(context: FieldForceRequestContext, visitId: UUID): PersistedFieldVisitReference {
        val created = requireNotNull(visits) { "FieldForce visit persistence is not configured" }
            .create(FieldVisitEntity(visitId, context.caseId, FieldVisitStatus.REQUESTED, context.requestedFor))
        return PersistedFieldVisitReference(created.id, created.caseId, created.status.name, created.scheduledAt, created.completedAt, created.outcome)
    }
}
