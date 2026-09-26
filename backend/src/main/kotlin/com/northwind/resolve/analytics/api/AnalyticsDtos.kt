package com.northwind.resolve.analytics.api

import java.math.BigDecimal
import java.time.Instant

data class DashboardMetrics(val backlog: Int, val slaCompliancePercent: BigDecimal, val firstContactResolutionPercent: BigDecimal, val transferRatePercent: BigDecimal, val reopenRatePercent: BigDecimal, val billingExceptionCount: Int, val estimatedReadRatePercent: BigDecimal)
data class DashboardKpisResponse(val generatedAt: Instant, val metrics: DashboardMetrics)
data class ValueCaseAssumptions(val inboundCallCost: BigDecimal, val standardComplaintCost: BigDecimal, val transferredComplaintCost: BigDecimal, val manualCorrectionCost: BigDecimal, val fieldVisitCost: BigDecimal, val contactCentreFteAnnualCost: BigDecimal, val buildCost: BigDecimal)
data class ValueCaseResponse(val assumptions: ValueCaseAssumptions, val observedMetrics: Map<String, BigDecimal>, val grossBenefit: BigDecimal, val annualOperatingCost: BigDecimal, val annualNetBenefit: BigDecimal, val paybackMonths: BigDecimal)
