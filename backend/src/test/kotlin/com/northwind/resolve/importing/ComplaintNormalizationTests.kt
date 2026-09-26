package com.northwind.resolve.importing

import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComplaintNormalizationTests {
    @Test
    fun `maps every approved raw complaint value deterministically`() {
        assertEquals(CasePriority.CRITICAL, ComplaintNormalization.priority("P1"))
        assertEquals(CasePriority.HIGH, ComplaintNormalization.priority("P2"))
        assertEquals(CasePriority.MEDIUM, ComplaintNormalization.priority("P3"))
        assertEquals(CaseCategory.BILLING, ComplaintNormalization.category("Billing - disputed amount"))
        assertEquals(CaseCategory.METERING, ComplaintNormalization.category("Metering - no read taken"))
        assertEquals(CaseCategory.SERVICE, ComplaintNormalization.category("Water - pressure or quality"))
        assertEquals(CaseCategory.OTHER, ComplaintNormalization.category("Other"))
        assertEquals(CaseStatus.OPEN, ComplaintNormalization.status("Open"))
        assertEquals(CaseStatus.CLOSED, ComplaintNormalization.status("Closed"))
        assertEquals(CaseStatus.IN_PROGRESS, ComplaintNormalization.status("Closed - reopened"))
    }

    @Test
    fun `rejects an unapproved raw value instead of silently inferring it`() {
        assertFailsWith<IllegalStateException> { ComplaintNormalization.category("Unmapped category") }
    }
}
