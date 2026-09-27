package com.northwind.resolve.audit.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.audit.api.*
import com.northwind.resolve.audit.domain.*
import com.northwind.resolve.audit.persistence.AuditEventRepository
import com.northwind.resolve.billing.domain.BillCorrectionEntity
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.application.MutationConflictException
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class AuditEventNotFoundException(id:String):RuntimeException("Audit event $id was not found")
@Service @Profile("database")
class AuditApplicationService(private val audits:AuditEventRepository, private val corrections:BillCorrectionRepository, private val cases:CaseWorkspaceService, private val idempotency:MutationIdempotencyRepository, private val mapper:ObjectMapper, private val solana:SolanaProvider) {
 private val hashing=AuditHashService()
 @Transactional fun createPendingForBillCorrection(caseId:String, correction:BillCorrectionEntity)=getOrCreate(caseId,correction)
 @Transactional fun create(request:CreateAuditEventRequest,key:String):AuditEventDto { require(key.length in 8..128){"Idempotency-Key must be between 8 and 128 characters"}; val fingerprint=MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(request)).joinToString(""){"%02x".format(it)}; idempotency.lock(OP,key); idempotency.find(OP,key)?.let{if(it.fingerprint!=fingerprint)throw MutationConflictException("Idempotency key was already used for a different audit request");return mapper.readValue(it.responseJson,AuditEventDto::class.java)}; require(request.eventType==AuditEventType.BILL_CORRECTED){"Unsupported audit event type"}; val case=cases.context(request.caseId).case; val correction=corrections.find(runCatching{UUID.fromString(request.eventReference)}.getOrElse{throw IllegalArgumentException("eventReference must be a correction UUID")})?:throw IllegalArgumentException("Bill correction was not found"); require(correction.accountId==case.accountId){"Bill correction does not belong to the case account"}; val dto=getOrCreate(request.caseId,correction); idempotency.store(OP,key,fingerprint,mapper.writeValueAsString(dto));return dto }
 fun list(caseId:String):List<AuditEventDto>{cases.context(caseId);return audits.list(caseId).map(::dto)}
 fun verify(id:UUID):AuditVerificationResponse { val audit=audits.find(id)?:throw AuditEventNotFoundException(id.toString()); if(audit.eventType!=AuditEventType.BILL_CORRECTED)return AuditVerificationResponse(id,AuditVerificationStatus.PENDING,Instant.now(),audit.payloadHash,null); val correction=audit.sourceEventReference?.let{runCatching{UUID.fromString(it)}.getOrNull()}?.let(corrections::find)?:return AuditVerificationResponse(id,AuditVerificationStatus.PENDING,Instant.now(),audit.payloadHash,null); val actual=hashing.hash(audit.caseId,correction); if(actual!=audit.payloadHash){audits.status(id,AuditStatus.FAILED);return AuditVerificationResponse(id,AuditVerificationStatus.MISMATCH,Instant.now(),audit.payloadHash,actual)}; val signature=audit.solanaSignature?:return AuditVerificationResponse(id,AuditVerificationStatus.PENDING,Instant.now(),audit.payloadHash,null); return when(solana.verify(signature,hashing.memo(audit.payloadHash))){SolanaAnchorVerification.MATCH->{audits.status(id,AuditStatus.VERIFIED);AuditVerificationResponse(id,AuditVerificationStatus.VERIFIED,Instant.now(),audit.payloadHash,audit.payloadHash)};SolanaAnchorVerification.MISMATCH->{audits.status(id,AuditStatus.FAILED);AuditVerificationResponse(id,AuditVerificationStatus.MISMATCH,Instant.now(),audit.payloadHash,audit.payloadHash)};SolanaAnchorVerification.PENDING->AuditVerificationResponse(id,AuditVerificationStatus.PENDING,Instant.now(),audit.payloadHash,null)} }
 private fun getOrCreate(caseId:String,c:BillCorrectionEntity):AuditEventDto {audits.findBySource(AuditEventType.BILL_CORRECTED,c.id.toString())?.let{return dto(it)}; val a=AuditEventEntity(caseId=caseId,eventType=AuditEventType.BILL_CORRECTED,payloadHash=hashing.hash(caseId,c),sourceEventReference=c.id.toString(),status=AuditStatus.PENDING,createdAt=Instant.now());audits.create(a);return dto(a)}
 private fun dto(a:AuditEventEntity)=AuditEventDto(a.id,a.caseId,a.eventType,a.payloadHash,a.solanaSignature,a.status,a.createdAt)
 companion object{const val OP="AUDIT_EVENT_CREATE"}
}
