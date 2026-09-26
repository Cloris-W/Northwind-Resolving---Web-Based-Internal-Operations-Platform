package com.northwind.resolve.cases.api

import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.persistence.CaseSearchCriteria
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("database")
@RequestMapping("/api/cases")
class CaseController(private val workspace: CaseWorkspaceService) {
    @GetMapping
    fun listCases(
        @RequestParam(required = false) status: CaseStatus?, @RequestParam(required = false) category: CaseCategory?,
        @RequestParam(required = false) region: String?, @RequestParam(required = false) accountId: String?,
        @RequestParam(required = false) slaBreached: Boolean?, @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
    ) = workspace.search(CaseSearchCriteria(status, category, region, accountId, slaBreached, page, size))

    @GetMapping("/{caseId}")
    fun caseContext(@PathVariable caseId: String) = workspace.context(caseId)

    @GetMapping("/{caseId}/timeline")
    fun timeline(@PathVariable caseId: String) = workspace.timeline(caseId)
}
