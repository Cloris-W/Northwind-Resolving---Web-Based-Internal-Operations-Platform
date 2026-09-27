package com.northwind.resolve.analytics.application

import com.northwind.resolve.analytics.persistence.AnalyticsReadRepository
import com.northwind.resolve.analytics.persistence.MonthlyKpiRow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.LocalDate

class AnalyticsServiceTests {
 private val repository=mock(AnalyticsReadRepository::class.java); private val service=AnalyticsService(repository)
 private fun givenSource(){val months=listOf(LocalDate.parse("2024-01-01"),LocalDate.parse("2024-02-01"));`when`(repository.latestMonths()).thenReturn(months);`when`(repository.monthlyKpis(months.first(),months.last())).thenReturn(listOf(MonthlyKpiRow(months[0],100,90,BigDecimal("0.80")),MonthlyKpiRow(months[1],200,190,BigDecimal("0.90"))));`when`(repository.complaintCounts(months.first(),months.last(),null)).thenReturn(Triple(10,2,4));`when`(repository.reopenedCount(months.first(),months.last(),null)).thenReturn(1);`when`(repository.backlog(null)).thenReturn(3);`when`(repository.billingExceptions(months.first(),months.last(),null)).thenReturn(2);`when`(repository.meterRows(months.first(),months.last(),null)).thenReturn(emptyList());`when`(repository.unitCost("Complaint handled end to end (average)")).thenReturn(BigDecimal("68"));`when`(repository.unitCost("Complaint handled end to end (transferred between systems)")).thenReturn(BigDecimal("121"));`when`(repository.correctionCount(months.first(),months.last())).thenReturn(9)}
 @Test fun `uses latest source months and weighted FCR`() {givenSource();val result=service.dashboard(null,null,null);assertEquals(LocalDate.parse("2024-01-01"),result.period.from);assertEquals(BigDecimal("86.67"),result.metrics.firstContactResolutionPercent);assertEquals("GLOBAL",result.traces.getValue("firstContactResolutionPercent").scope)}
 @Test fun `returns null model outputs for incomplete scenario`() {givenSource();val result=service.valueCase(null,null,null,null);assertNull(result.grossBenefit);assertNull(result.paybackMonths);assertEquals(9,result.observedBaseline.manualCorrectionCount)}
 @Test fun `calculates core benefit without correction monetization`() {givenSource();val result=service.valueCase(BigDecimal("10"),BigDecimal("20"),BigDecimal("100"),BigDecimal("1000"));assertEquals(BigDecimal("2040.00"),result.projectedComplaintBenefit);assertEquals(BigDecimal("38.16"),result.projectedTransferBenefit);assertEquals(BigDecimal("2078.16"),result.grossBenefit)}
}
