package com.northwind.resolve.billing.application

import com.northwind.resolve.billing.domain.RiskLevel

data class RiskInput(val estimatedRead: Boolean, val deviationFromHistoryOver50Percent: Boolean, val consecutiveEstimatesAtLeast3: Boolean, val previousBillCorrections: Boolean, val previousBillingComplaints: Boolean)
data class RiskAssessment(val score: Int, val level: RiskLevel, val reasonCodes: List<String>)

class BillingRiskEngine {
    fun assess(input: RiskInput): RiskAssessment {
        val reasons = mutableListOf<String>(); var score = 0
        fun add(condition: Boolean, points: Int, code: String) { if (condition) { score += points; reasons += code } }
        add(input.estimatedRead, 25, "ESTIMATED_READ")
        add(input.deviationFromHistoryOver50Percent, 30, "DEVIATION_OVER_50_PERCENT")
        add(input.consecutiveEstimatesAtLeast3, 20, "CONSECUTIVE_ESTIMATES_AT_LEAST_3")
        add(input.previousBillCorrections, 15, "PREVIOUS_BILL_CORRECTION")
        add(input.previousBillingComplaints, 10, "PREVIOUS_BILLING_COMPLAINT")
        return RiskAssessment(score, when { score >= 60 -> RiskLevel.HIGH; score >= 30 -> RiskLevel.MEDIUM; else -> RiskLevel.LOW }, reasons)
    }
}
