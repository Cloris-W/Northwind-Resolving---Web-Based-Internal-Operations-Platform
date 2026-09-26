package com.northwind.resolve.cases.api

import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.domain.SourceSystem
import com.northwind.resolve.common.api.PageMetadata
import com.northwind.resolve.billing.api.BillingExceptionDto
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import com.northwind.resolve.metering.api.MeterReadingDto
import java.time.Instant
import java.util.UUID

data class CaseDto(val caseId: String, val accountId: String, val category: CaseCategory, val priority: CasePriority, val region: String, val status: CaseStatus, val slaDays: Int, val openedAt: Instant, val closedAt: Instant?, val assignedTeam: String)
data class CaseEventDto(val eventId: UUID, val caseId: String, val eventType: CaseEventType, val sourceSystem: SourceSystem, val timestamp: Instant, val actor: String, val description: String, val metadata: Map<String, Any?>? = null)
data class CaseListResponse(val items: List<CaseDto>, val page: PageMetadata)
data class CaseTimelineResponse(val caseId: String, val events: List<CaseEventDto>)
data class TransferCaseRequest(val assignedTeam: String, val reason: String? = null)
data class TransferCaseResponse(val case: CaseDto, val event: CaseEventDto)
data class CaseContextResponse(
    val case: CaseDto,
    val latestBillingException: BillingExceptionDto?,
    val latestMeterReading: MeterReadingDto?,
    val fieldVisits: List<FieldVisitDto>,
)
