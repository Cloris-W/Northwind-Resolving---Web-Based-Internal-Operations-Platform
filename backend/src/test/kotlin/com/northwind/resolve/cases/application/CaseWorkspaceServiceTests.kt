package com.northwind.resolve.cases.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.billing.persistence.BillingHistoryReadRepository
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseEntity
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.persistence.CaseWorkspaceRepository
import com.northwind.resolve.integrations.FieldForceProvider
import com.northwind.resolve.metering.persistence.MeterReadingReadRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant

class CaseWorkspaceServiceTests {
    @Test
    fun `maps canonical case entity without exposing imported raw complaint fields`() {
        val service = CaseWorkspaceService(mock(CaseWorkspaceRepository::class.java), mock(FieldForceProvider::class.java), mock(BillingHistoryReadRepository::class.java), mock(MeterReadingReadRepository::class.java), ObjectMapper())
        val dto = service.caseDto(CaseEntity("NW-100001", "ACC-943644", "SYS-01", CaseCategory.BILLING, CasePriority.MEDIUM, "Ashford", CaseStatus.CLOSED, 20, Instant.parse("2024-10-01T00:00:00Z"), Instant.parse("2024-10-30T00:00:00Z"), "UNASSIGNED_IMPORT"))
        assertEquals("NW-100001", dto.caseId)
        assertEquals("ACC-943644", dto.accountId)
        assertEquals(CaseCategory.BILLING, dto.category)
    }
}
