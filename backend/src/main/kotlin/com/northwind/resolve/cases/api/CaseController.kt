package com.northwind.resolve.cases.api

import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.application.CaseTransferService
import com.northwind.resolve.cases.application.InvalidCaseTransferException
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.persistence.CaseSearchCriteria
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import com.northwind.resolve.fieldforce.api.FieldVisitRequest
import com.northwind.resolve.fieldforce.application.FieldVisitService

@RestController
@Profile("database")
@RequestMapping("/api/cases")
class CaseController(
    private val workspace: CaseWorkspaceService,
    private val transfers: CaseTransferService,
    private val fieldVisits: FieldVisitService,
) {
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

    @PostMapping("/{caseId}/transfer")
    fun transfer(
        @PathVariable caseId: String,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestBody request: TransferCaseRequest,
    ) = transfers.transfer(caseId, request, idempotencyKey ?: throw InvalidCaseTransferException("Idempotency-Key is required"))

    @PostMapping("/{caseId}/field-visit")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestFieldVisit(
        @PathVariable caseId: String,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestBody request: FieldVisitRequest,
    ) = fieldVisits.request(caseId, request, idempotencyKey ?: throw com.northwind.resolve.cases.application.InvalidRequestException("Idempotency-Key is required"))
}
