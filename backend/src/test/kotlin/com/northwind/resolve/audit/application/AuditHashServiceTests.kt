package com.northwind.resolve.audit.application

import com.northwind.resolve.billing.domain.BillCorrectionEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AuditHashServiceTests {
 private val service=AuditHashService()
 private fun correction(value:String="12.3400")=BillCorrectionEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"),"account-x",BigDecimal(value),BigDecimal("10.0000"),"reason","region",Instant.parse("2026-01-01T00:00:00Z"))
 @Test fun `hash is deterministic and memo is exact safe length`(){val hash=service.hash("case-x",correction());assertEquals(hash,service.hash("case-x",correction()));assertEquals(64,hash.length);val memo=service.memo(hash);assertEquals(98,memo.toByteArray().size);assertEquals("northwind-resolve:audit:v1:sha256:",memo.take(34));assertFalse(memo.contains("case-x"));assertFalse(memo.contains("account-x"));assertFalse(memo.contains("12.3400"));assertFalse(memo.contains("reason"))}
 @Test fun `canonical payload preserves plain decimal and UTC instant`(){val payload=service.payload("case-x",correction());assertTrue(payload.contains("12.3400"));assertTrue(payload.contains("2026-01-01T00:00:00Z"));assertNotEquals(service.hash("case-x",correction()),service.hash("case-x",correction("13.3400")))}
}
