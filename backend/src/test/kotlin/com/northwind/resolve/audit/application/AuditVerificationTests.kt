package com.northwind.resolve.audit.application
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.northwind.resolve.audit.domain.*
import com.northwind.resolve.audit.persistence.AuditEventRepository
import com.northwind.resolve.billing.domain.BillCorrectionEntity
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
class AuditVerificationTests {private val audits=mock(AuditEventRepository::class.java);private val corrections=mock(BillCorrectionRepository::class.java);private val provider=mock(SolanaProvider::class.java);private val id=UUID.randomUUID();private val correction=BillCorrectionEntity(UUID.randomUUID(),"account",BigDecimal.ONE,BigDecimal.TEN,"reason","region",Instant.EPOCH);private val service=AuditApplicationService(audits,corrections,mock(CaseWorkspaceService::class.java),mock(MutationIdempotencyRepository::class.java),jacksonObjectMapper(),provider);private val hash=AuditHashService().hash("case",correction);private val audit=AuditEventEntity(id,"case",AuditEventType.BILL_CORRECTED,hash,"signature",correction.id.toString(),AuditStatus.SUBMITTED,Instant.EPOCH);private fun given(){`when`(audits.find(id)).thenReturn(audit);`when`(corrections.find(correction.id)).thenReturn(correction)}
 @Test fun `verified anchor is verified`(){given();`when`(provider.verify("signature",AuditHashService().memo(hash))).thenReturn(SolanaAnchorVerification.MATCH);assertEquals(AuditVerificationStatus.VERIFIED,service.verify(id).verificationStatus)}
 @Test fun `mismatched anchor is mismatch`(){given();`when`(provider.verify(anyString(),anyString())).thenReturn(SolanaAnchorVerification.MISMATCH);assertEquals(AuditVerificationStatus.MISMATCH,service.verify(id).verificationStatus)}
 @Test fun `unavailable anchor is pending`(){given();`when`(provider.verify(anyString(),anyString())).thenReturn(SolanaAnchorVerification.PENDING);assertEquals(AuditVerificationStatus.PENDING,service.verify(id).verificationStatus)}}
