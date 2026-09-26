package com.northwind.resolve.audit.api

import com.northwind.resolve.audit.domain.AuditEventType
import com.northwind.resolve.audit.domain.AuditStatus
import com.northwind.resolve.audit.domain.AuditVerificationStatus
import java.time.Instant
import java.util.UUID

data class CreateAuditEventRequest(val caseId: String, val eventType: AuditEventType, val eventReference: String)
data class AuditEventDto(val id: UUID, val caseId: String, val eventType: AuditEventType, val payloadHash: String, val solanaSignature: String?, val status: AuditStatus, val createdAt: Instant)
data class AuditVerificationResponse(val auditEventId: UUID, val verificationStatus: AuditVerificationStatus, val checkedAt: Instant, val expectedHash: String?, val actualHash: String?)
