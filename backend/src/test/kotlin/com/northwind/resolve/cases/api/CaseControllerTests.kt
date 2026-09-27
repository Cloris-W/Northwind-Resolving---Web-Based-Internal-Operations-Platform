package com.northwind.resolve.cases.api

import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.application.CaseTransferService
import com.northwind.resolve.fieldforce.application.FieldVisitService
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.persistence.CaseSearchCriteria
import com.northwind.resolve.common.api.ApiExceptionHandler
import com.northwind.resolve.common.api.PageMetadata
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(CaseController::class)
@ActiveProfiles("database")
@Import(ApiExceptionHandler::class)
class CaseControllerTests {
    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var workspace: CaseWorkspaceService
    @MockitoBean lateinit var transfers: CaseTransferService
    @MockitoBean lateinit var fieldVisits: FieldVisitService

    @Test fun `returns canonical case search results`() {
        val case = CaseDto("NW-100001", "ACC-943644", CaseCategory.BILLING, CasePriority.MEDIUM, "Ashford", CaseStatus.CLOSED, 20, Instant.parse("2024-10-01T00:00:00Z"), Instant.parse("2024-10-30T00:00:00Z"), "UNASSIGNED_IMPORT")
        `when`(workspace.search(CaseSearchCriteria(null, null, null, "ACC-943644", null, 0, 25))).thenReturn(CaseListResponse(listOf(case), PageMetadata(0, 25, 1, 1)))
        mockMvc.perform(get("/api/cases").param("accountId", "ACC-943644")).andExpect(status().isOk).andExpect(jsonPath("$.items[0].caseId").value("NW-100001")).andExpect(jsonPath("$.items[0].category").value("BILLING"))
    }

    @Test fun `returns contract error for missing case`() {
        `when`(workspace.context("missing")).thenThrow(CaseNotFoundException("missing"))
        mockMvc.perform(get("/api/cases/missing")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("CASE_NOT_FOUND")).andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test fun `transfers a case through the approved endpoint`() {
        val case = CaseDto("NW-100001", "ACC-943644", CaseCategory.BILLING, CasePriority.MEDIUM, "Ashford", CaseStatus.CLOSED, 20, Instant.parse("2024-10-01T00:00:00Z"), null, "RESOLUTION")
        val event = CaseEventDto(java.util.UUID.randomUUID(), "NW-100001", com.northwind.resolve.cases.domain.CaseEventType.TRANSFERRED, com.northwind.resolve.cases.domain.SourceSystem.RESOLVE, Instant.parse("2025-01-01T00:00:00Z"), "SYSTEM_GENERATED", "Case transferred to RESOLUTION")
        `when`(transfers.transfer("NW-100001", TransferCaseRequest("RESOLUTION", "handoff"), "transfer-key-1")).thenReturn(TransferCaseResponse(case, event))
        mockMvc.perform(post("/api/cases/NW-100001/transfer").header("Idempotency-Key", "transfer-key-1").contentType("application/json").content("""{"assignedTeam":"RESOLUTION","reason":"handoff"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.case.caseId").value("NW-100001")).andExpect(jsonPath("$.event.eventType").value("TRANSFERRED"))
    }

    @Test fun `returns standard transfer error when idempotency key is missing`() {
        mockMvc.perform(post("/api/cases/NW-100001/transfer").contentType("application/json").content("""{"assignedTeam":"RESOLUTION"}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_CASE_TRANSFER")).andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test fun `requests a field visit through the approved endpoint`() {
        val visit = FieldVisitDto(java.util.UUID.randomUUID(), "NW-100001", FieldVisitStatus.REQUESTED, Instant.parse("2025-01-02T10:00:00Z"), null, null)
        `when`(fieldVisits.request("NW-100001", com.northwind.resolve.fieldforce.api.FieldVisitRequest(Instant.parse("2025-01-02T10:00:00Z"), "Inspect meter", null, null), "visit-key-001")).thenReturn(visit)
        mockMvc.perform(post("/api/cases/NW-100001/field-visit").header("Idempotency-Key", "visit-key-001").contentType("application/json").content("""{"requestedFor":"2025-01-02T10:00:00Z","visitReason":"Inspect meter"}"""))
            .andExpect(status().isCreated).andExpect(jsonPath("$.caseId").value("NW-100001")).andExpect(jsonPath("$.status").value("REQUESTED"))
    }
}
