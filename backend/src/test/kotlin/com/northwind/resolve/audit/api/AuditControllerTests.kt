package com.northwind.resolve.audit.api
import com.northwind.resolve.audit.application.AuditApplicationService
import com.northwind.resolve.audit.domain.*
import com.northwind.resolve.common.api.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID
@WebMvcTest(AuditController::class) @ActiveProfiles("database") @Import(ApiExceptionHandler::class)
class AuditControllerTests {@Autowired lateinit var mvc:MockMvc;@MockitoBean lateinit var audits:AuditApplicationService;private val dto=AuditEventDto(UUID.randomUUID(),"case",AuditEventType.BILL_CORRECTED,"a".repeat(64),null,AuditStatus.PENDING,Instant.EPOCH);private val reference=UUID.randomUUID().toString()
 @Test fun `post returns accepted`(){`when`(audits.create(CreateAuditEventRequest("case",AuditEventType.BILL_CORRECTED,reference),"audit-key-1")).thenReturn(dto);mvc.perform(post("/api/audit/events").header("Idempotency-Key","audit-key-1").contentType("application/json").content("""{"caseId":"case","eventType":"BILL_CORRECTED","eventReference":"$reference"}""")).andExpect(status().isAccepted).andExpect(jsonPath("$.id").value(dto.id.toString()))}
 @Test fun `list returns ok`(){`when`(audits.list("case")).thenReturn(listOf(dto));mvc.perform(get("/api/audit/events").param("caseId","case")).andExpect(status().isOk).andExpect(jsonPath("$[0].caseId").value("case"))}
 @Test fun `verify returns ok`(){`when`(audits.verify(dto.id)).thenReturn(AuditVerificationResponse(dto.id,AuditVerificationStatus.PENDING,Instant.EPOCH,dto.payloadHash,null));mvc.perform(get("/api/audit/events/${dto.id}/verify")).andExpect(status().isOk).andExpect(jsonPath("$.verificationStatus").value("PENDING"))}}
