package com.northwind.resolve.audit.application

import com.northwind.resolve.billing.domain.BillCorrectionEntity
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class AuditHashService {
    fun payload(caseId:String, correction:BillCorrectionEntity) = """{"version":"northwind-resolve-audit-v1","eventType":"BILL_CORRECTED","sourceEventReference":"${correction.id}","caseId":"$caseId","accountId":"${correction.accountId}","originalValue":"${correction.originalValue.toPlainString()}","correctedValue":"${correction.correctedValue.toPlainString()}","reason":${json(correction.reason)},"region":${json(correction.region)},"createdAt":"${correction.createdAt}"}"""
    fun hash(caseId:String, correction:BillCorrectionEntity)=MessageDigest.getInstance("SHA-256").digest(payload(caseId,correction).toByteArray(StandardCharsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun memo(hash:String)="northwind-resolve:audit:v1:sha256:$hash"
    private fun json(value:String)="\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""
}
