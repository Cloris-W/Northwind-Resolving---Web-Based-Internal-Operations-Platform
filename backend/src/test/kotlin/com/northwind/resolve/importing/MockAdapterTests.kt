package com.northwind.resolve.importing

import com.northwind.resolve.integrations.BillingMockAdapter
import com.northwind.resolve.integrations.CaseTrackMockAdapter
import com.northwind.resolve.integrations.FieldForceMockAdapter
import com.northwind.resolve.integrations.MeterHubMockAdapter
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MockAdapterTests {
    private val source = object : LegacySourceQuery {
        override fun findCase(caseId: String) = if (caseId == "NW-100001") ImportedCaseReference(caseId, "ACC-1", "SYS-04", "Open") else null
        override fun findCasesByAccount(accountId: String) = listOf(ImportedCaseReference("NW-100001", accountId, "SYS-04", "Open"))
        override fun findMeterMetrics(region: String, month: LocalDate) = listOf(MeterRegionMetric(month, region, 10, BigDecimal("0.1"), BigDecimal("0.2"), 1, "MeterHub"))
        override fun findBillingIndicators(accountId: String) = listOf(BillingCorrectionIndicator("NW-100001", accountId, BigDecimal("12.50")))
        override fun findFieldVisits(caseId: String) = emptyList<PersistedFieldVisitReference>()
        override fun findBillingRiskFacts() = emptyList<ImportedBillingRiskFact>()
    }

    @Test
    fun `mock adapters expose imported source data and no fabricated field visits`() {
        assertEquals("ACC-1", CaseTrackMockAdapter(source).findCase("NW-100001")?.accountId)
        assertNull(CaseTrackMockAdapter(source).findCase("missing"))
        assertEquals(1, MeterHubMockAdapter(source).findRegionalMetric("North", LocalDate.of(2025, 1, 1)).size)
        assertEquals(1, BillingMockAdapter(source).findCorrectionIndicators("ACC-1").size)
        assertEquals(emptyList(), FieldForceMockAdapter(source).findPersistedVisits("NW-100001"))
    }
}
