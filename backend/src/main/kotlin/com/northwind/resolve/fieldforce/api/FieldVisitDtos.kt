package com.northwind.resolve.fieldforce.api

import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import java.time.Instant
import java.util.UUID

data class FieldVisitRequest(val requestedFor: Instant, val visitReason: String, val meterId: String? = null, val instructions: String? = null)
data class FieldVisitDto(val id: UUID, val caseId: String, val status: FieldVisitStatus, val scheduledAt: Instant, val completedAt: Instant?, val outcome: String?)
