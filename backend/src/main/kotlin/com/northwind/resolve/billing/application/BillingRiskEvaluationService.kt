package com.northwind.resolve.billing.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.billing.domain.BillingExceptionEntity
import com.northwind.resolve.billing.domain.BillingExceptionStatus
import com.northwind.resolve.billing.domain.RiskLevel
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.billing.persistence.BillingExceptionRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service @Profile("database")
class BillingRiskEvaluationService(private val source: BillingRiskSourcePort, private val corrections: BillCorrectionRepository, private val exceptions: BillingExceptionRepository, private val objectMapper: ObjectMapper) {
    private val engine = BillingRiskEngine()
    @Transactional fun generateImportedExceptions() { source.candidates().forEach { candidate ->
        if (exceptions.existsForCase(candidate.caseId)) return@forEach
        val assessment=engine.assess(RiskInput(false,false,false,candidate.legacyEarlierCorrectionEvidence || corrections.existsEarlier(candidate.accountId, Instant.now()),candidate.previousBillingComplaints))
        if (assessment.level == RiskLevel.HIGH) exceptions.create(BillingExceptionEntity(accountId=candidate.accountId,caseId=candidate.caseId,riskScore=assessment.score,riskLevel=assessment.level,reasonCodes=objectMapper.writeValueAsString(assessment.reasonCodes),status=BillingExceptionStatus.OPEN,region=candidate.region,createdAt=Instant.now()))
    } }
}
