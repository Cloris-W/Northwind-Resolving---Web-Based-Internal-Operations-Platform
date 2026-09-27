package com.northwind.resolve.billing.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.billing.api.*
import com.northwind.resolve.billing.domain.*
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.billing.persistence.BillingExceptionRepository
import com.northwind.resolve.cases.application.CaseTimelineEventService
import com.northwind.resolve.cases.application.MutationConflictException
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import com.northwind.resolve.fieldforce.application.FieldVisitService
import com.northwind.resolve.audit.application.AuditApplicationService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service @Profile("database")
class BillingExceptionReviewService(private val exceptions: BillingExceptionRepository, private val corrections: BillCorrectionRepository, private val timeline: CaseTimelineEventService, private val fieldVisits: FieldVisitService, private val idempotency: MutationIdempotencyRepository, private val objectMapper: ObjectMapper, private val audits: AuditApplicationService) {
    @Transactional fun review(id: UUID, request: BillingExceptionReviewRequest, key: String): BillingExceptionReviewResponse {
        validate(request,key); val fingerprint=digest("$id",objectMapper.writeValueAsString(request)); idempotency.lock(OP,key)
        idempotency.find(OP,key)?.let { if(it.fingerprint != fingerprint) throw MutationConflictException("Idempotency key was already used for a different billing review"); return objectMapper.readValue(it.responseJson,BillingExceptionReviewResponse::class.java) }
        val exception=exceptions.find(id)?:throw BillingExceptionNotFoundException(id.toString())
        if(exception.status !in allowed(request.action)) throw MutationConflictException("Billing exception cannot be reviewed in its current status")
        var correctionDto: BillCorrectionDto?=null; var visitDto: com.northwind.resolve.fieldforce.api.FieldVisitDto?=null
        when(request) {
            is VerifyReadingReviewRequest -> { exceptions.updateStatus(id,BillingExceptionStatus.RESOLVED,request.reviewedBy); timeline.appendIfCaseExists(exception.caseId,CaseEventType.BILL_CHECKED,request.reviewedBy,"Billing reading verified") }
            is ApproveReviewRequest -> { exceptions.updateStatus(id,BillingExceptionStatus.DISMISSED,request.reviewedBy); timeline.appendIfCaseExists(exception.caseId,CaseEventType.BILL_CHECKED,request.reviewedBy,"Original bill approved as valid") }
            is CorrectBillReviewRequest -> { val d=request.correction; val c=BillCorrectionEntity(UUID.randomUUID(),exception.accountId,d.originalValue,d.correctedValue,d.reason,d.region,Instant.now()); corrections.create(c); correctionDto=BillCorrectionDto(c.id,c.accountId,c.originalValue,c.correctedValue,c.reason,c.region,c.createdAt); exceptions.updateStatus(id,BillingExceptionStatus.RESOLVED,request.reviewedBy); timeline.appendIfCaseExists(exception.caseId,CaseEventType.BILL_CORRECTED,request.reviewedBy,"Bill correction recorded"); if(exception.caseId!=null) audits.createPendingForBillCorrection(exception.caseId!!,c) }
            is RequestFieldVisitReviewRequest -> { if(!timeline.exists(exception.caseId)) throw MutationConflictException("A valid related case is required for a field visit"); exceptions.updateStatus(id,BillingExceptionStatus.IN_REVIEW,request.reviewedBy); visitDto=fieldVisits.request(exception.caseId!!,request.fieldVisitRequest,key) }
        }
        val updated=exceptions.find(id)!!; val response=BillingExceptionReviewResponse(updated.toDto(),request.action,correctionDto,visitDto,Instant.now()); idempotency.store(OP,key,fingerprint,objectMapper.writeValueAsString(response)); return response
    }
    private fun validate(request: BillingExceptionReviewRequest,key:String) { if(key.length !in 8..128) throw IllegalArgumentException("Idempotency-Key must be between 8 and 128 characters"); if(request.reviewedBy.isBlank()||request.reviewedBy.length>128) throw IllegalArgumentException("reviewedBy must be between 1 and 128 characters"); if(request.notes?.length?:0>1000) throw IllegalArgumentException("notes must not exceed 1000 characters") }
    private fun allowed(action: BillingReviewAction)=when(action){ BillingReviewAction.REQUEST_FIELD_VISIT->setOf(BillingExceptionStatus.OPEN); else->setOf(BillingExceptionStatus.OPEN,BillingExceptionStatus.IN_REVIEW) }
    companion object { const val OP="BILLING_EXCEPTION_REVIEW" }
}
internal fun BillingExceptionEntity.toDto() = BillingExceptionDto(id,accountId,caseId,riskScore,riskLevel,com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readValue(reasonCodes, List::class.java) as List<String>,status,region,createdAt,reviewedBy)
private fun digest(vararg values:String)=MessageDigest.getInstance("SHA-256").digest(values.joinToString("\u001f").toByteArray(StandardCharsets.UTF_8)).joinToString(""){"%02x".format(it)}
