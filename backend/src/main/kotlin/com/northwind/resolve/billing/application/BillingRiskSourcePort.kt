package com.northwind.resolve.billing.application

import java.time.Instant

data class BillingRiskCandidate(val caseId: String, val accountId: String, val region: String, val sourceOpenedAt: Instant, val legacyEarlierCorrectionEvidence: Boolean, val previousBillingComplaints: Boolean)
interface BillingRiskSourcePort { fun candidates(): List<BillingRiskCandidate> }
