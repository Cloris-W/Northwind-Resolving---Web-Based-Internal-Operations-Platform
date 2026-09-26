package com.northwind.resolve.cases.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.billing.api.BillingExceptionDto
import com.northwind.resolve.billing.persistence.BillingHistoryReadRepository
import com.northwind.resolve.cases.api.CaseContextResponse
import com.northwind.resolve.cases.api.CaseDto
import com.northwind.resolve.cases.api.CaseEventDto
import com.northwind.resolve.cases.api.CaseListResponse
import com.northwind.resolve.cases.api.CaseTimelineResponse
import com.northwind.resolve.cases.persistence.CaseSearchCriteria
import com.northwind.resolve.cases.persistence.CaseWorkspaceRepository
import com.northwind.resolve.common.api.PageMetadata
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import com.northwind.resolve.integrations.FieldForceProvider
import com.northwind.resolve.metering.api.MeterReadingDto
import com.northwind.resolve.metering.persistence.MeterReadingReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("database")
class CaseWorkspaceService(
    private val cases: CaseWorkspaceRepository,
    private val fieldForce: FieldForceProvider,
    private val billing: BillingHistoryReadRepository,
    private val meterReadings: MeterReadingReadRepository,
    private val objectMapper: ObjectMapper,
) {
    fun search(criteria: CaseSearchCriteria): CaseListResponse {
        validatePaging(criteria.page, criteria.size)
        val result = cases.search(criteria)
        return CaseListResponse(result.items.map(::caseDto), PageMetadata(criteria.page, criteria.size, result.totalElements, ((result.totalElements + criteria.size - 1) / criteria.size).toInt()))
    }

    fun context(caseId: String): CaseContextResponse {
        val case = cases.findCase(caseId) ?: throw CaseNotFoundException(caseId)
        val visits = fieldForce.findPersistedVisits(caseId).map { FieldVisitDto(it.visitId, it.caseId, FieldVisitStatus.valueOf(it.status), it.scheduledAt, it.completedAt, it.outcome) }
        return CaseContextResponse(caseDto(case), billing.findLatestException(caseId)?.let(::billingDto), meterReadings.findLatestForCaseAccount(case.accountId)?.let(::meterDto), visits)
    }

    fun timeline(caseId: String): CaseTimelineResponse {
        if (cases.findCase(caseId) == null) throw CaseNotFoundException(caseId)
        return CaseTimelineResponse(caseId, cases.findEvents(caseId).map { event ->
            CaseEventDto(event.eventId, event.caseId, event.eventType, event.sourceSystem, event.occurredAt, event.actor, event.description, event.metadataJson?.let { objectMapper.readValue(it, Map::class.java) as Map<String, Any?> })
        })
    }

    fun accountExists(accountId: String) = cases.accountExists(accountId)

    private fun validatePaging(page: Int, size: Int) {
        if (page < 0 || size !in 1..100) throw InvalidRequestException("page must be at least 0 and size must be between 1 and 100")
    }

    internal fun caseDto(case: com.northwind.resolve.cases.domain.CaseEntity) = CaseDto(case.caseId, case.accountId, case.category, case.priority, case.region, case.status, case.slaDays, case.openedAt, case.closedAt, case.assignedTeam)
    private fun meterDto(reading: com.northwind.resolve.metering.domain.MeterReadingEntity) = MeterReadingDto(reading.accountId, reading.meterId, reading.readingAt, reading.value, reading.estimated, reading.region)
    private fun billingDto(exception: com.northwind.resolve.billing.domain.BillingExceptionEntity) = BillingExceptionDto(exception.id, exception.accountId, exception.caseId, exception.riskScore, exception.riskLevel, objectMapper.readValue(exception.reasonCodes, List::class.java) as List<String>, exception.status, exception.region, exception.createdAt, exception.reviewedBy)
}
