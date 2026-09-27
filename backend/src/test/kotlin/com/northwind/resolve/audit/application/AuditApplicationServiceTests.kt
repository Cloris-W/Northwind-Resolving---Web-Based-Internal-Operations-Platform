package com.northwind.resolve.audit.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.northwind.resolve.audit.api.CreateAuditEventRequest
import com.northwind.resolve.audit.domain.*
import com.northwind.resolve.audit.persistence.AuditEventRepository
import com.northwind.resolve.billing.domain.BillCorrectionEntity
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.cases.api.CaseContextResponse
import com.northwind.resolve.cases.api.CaseDto
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.domain.*
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AuditApplicationServiceTests {
 private val audits=mock(AuditEventRepository::class.java); private val corrections=mock(BillCorrectionRepository::class.java); private val cases=mock(CaseWorkspaceService::class.java); private val idem=mock(MutationIdempotencyRepository::class.java); private val provider=mock(SolanaProvider::class.java)
 private val service=AuditApplicationService(audits,corrections,cases,idem,jacksonObjectMapper(),provider)
 private val id=UUID.randomUUID(); private fun correction()=BillCorrectionEntity(id,"account",BigDecimal("12.3400"),BigDecimal.TEN,"reason","region",Instant.parse("2026-01-01T00:00:00Z")); private fun context()=CaseContextResponse(CaseDto("case","account",CaseCategory.BILLING,CasePriority.HIGH,"region",CaseStatus.OPEN,1,Instant.EPOCH,null,"team"),null,null,emptyList())
 @Test fun `creates pending audit with source reference`(){val c=correction();`when`(audits.findBySource(AuditEventType.BILL_CORRECTED,id.toString())).thenReturn(null);val result=service.createPendingForBillCorrection("case",c);assertEquals(AuditStatus.PENDING,result.status);assertEquals(64,result.payloadHash.length);assertEquals(id.toString(),mockingDetails(audits).invocations.first{v->v.method.name=="create"}.arguments[0].let{it as AuditEventEntity}.sourceEventReference)}
 @Test fun `duplicate source reuses same audit`(){val existing=AuditEventEntity(UUID.randomUUID(),"case",AuditEventType.BILL_CORRECTED,"a".repeat(64),null,id.toString(),AuditStatus.PENDING,Instant.EPOCH);`when`(audits.findBySource(AuditEventType.BILL_CORRECTED,id.toString())).thenReturn(existing);assertEquals(existing.id,service.createPendingForBillCorrection("case",correction()).id);assertTrue(mockingDetails(audits).invocations.none{it.method.name=="create"})}
 @Test fun `unsupported direct event is rejected`(){assertThrows(IllegalArgumentException::class.java){service.create(CreateAuditEventRequest("case",AuditEventType.CASE_CLOSED,id.toString()),"audit-key-1")}}
}
