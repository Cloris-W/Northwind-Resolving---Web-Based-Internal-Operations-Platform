package com.northwind.resolve.integrations

import com.northwind.resolve.billing.application.BillingRiskCandidate
import com.northwind.resolve.billing.application.BillingRiskSourcePort
import com.northwind.resolve.importing.LegacySourceQuery
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component @Profile("database")
class ImportedBillingRiskSourceAdapter(private val source: LegacySourceQuery) : BillingRiskSourcePort {
    override fun candidates() = source.findBillingRiskFacts().map { BillingRiskCandidate(it.caseId,it.accountId,it.region,it.openedAt,it.earlierCorrectionEvidence,it.earlierBillingComplaint) }
}
