package com.northwind.resolve.cases.api

import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.CaseWorkspaceService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(CaseController::class)
@ActiveProfiles("database")
@Import(ApiExceptionHandler::class)
class CaseControllerTests {
    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var workspace: CaseWorkspaceService

    @Test fun `returns canonical case search results`() {
        val case = CaseDto("NW-100001", "ACC-943644", CaseCategory.BILLING, CasePriority.MEDIUM, "Ashford", CaseStatus.CLOSED, 20, Instant.parse("2024-10-01T00:00:00Z"), Instant.parse("2024-10-30T00:00:00Z"), "UNASSIGNED_IMPORT")
        `when`(workspace.search(CaseSearchCriteria(null, null, null, "ACC-943644", null, 0, 25))).thenReturn(CaseListResponse(listOf(case), PageMetadata(0, 25, 1, 1)))
        mockMvc.perform(get("/api/cases").param("accountId", "ACC-943644")).andExpect(status().isOk).andExpect(jsonPath("$.items[0].caseId").value("NW-100001")).andExpect(jsonPath("$.items[0].category").value("BILLING"))
    }

    @Test fun `returns contract error for missing case`() {
        `when`(workspace.context("missing")).thenThrow(CaseNotFoundException("missing"))
        mockMvc.perform(get("/api/cases/missing")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("CASE_NOT_FOUND")).andExpect(jsonPath("$.traceId").isNotEmpty)
    }
}
