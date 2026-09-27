package com.northwind.resolve.analytics.api

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class AnalyticsPeriod(val from: LocalDate, val to: LocalDate)
data class DashboardMetrics(val backlog: Int, val slaCompliancePercent: BigDecimal?, val firstContactResolutionPercent: BigDecimal?, val transferRatePercent: BigDecimal?, val reopenRatePercent: BigDecimal?, val billingExceptionCount: Int, val estimatedReadRatePercent: BigDecimal?)
data class DashboardTrendPoint(val month: LocalDate, val complaintsOpened: Int, val complaintsClosed: Int, val firstContactResolutionPercent: BigDecimal?, val scope: String = "GLOBAL")
data class EstimatedReadTrendPoint(val month: LocalDate, val estimatedReadRatePercent: BigDecimal?, val scope: String)
data class MetricTrace(val source: String, val period: AnalyticsPeriod, val classification: String, val scope: String, val formula: String? = null)
data class DashboardKpisResponse(val generatedAt: Instant, val period: AnalyticsPeriod, val metrics: DashboardMetrics, val trends: List<DashboardTrendPoint>, val estimatedReadTrend: List<EstimatedReadTrendPoint>, val traces: Map<String, MetricTrace>)
data class ValueCaseAssumptions(val complaintReductionPercent: BigDecimal?, val transferReductionPercent: BigDecimal?, val annualOperatingCost: BigDecimal?, val implementationCost: BigDecimal?)
data class ValueCaseObservedBaseline(val annualComplaints: Int, val annualTransferredComplaints: Int, val averageComplaintCost: BigDecimal?, val transferredComplaintCost: BigDecimal?, val manualCorrectionCount: Int)
data class ValueCaseResponse(val period: AnalyticsPeriod, val assumptions: ValueCaseAssumptions, val observedBaseline: ValueCaseObservedBaseline, val projectedComplaintBenefit: BigDecimal?, val projectedTransferBenefit: BigDecimal?, val grossBenefit: BigDecimal?, val annualOperatingCost: BigDecimal?, val annualNetBenefit: BigDecimal?, val paybackMonths: BigDecimal?, val traces: Map<String, MetricTrace>)
