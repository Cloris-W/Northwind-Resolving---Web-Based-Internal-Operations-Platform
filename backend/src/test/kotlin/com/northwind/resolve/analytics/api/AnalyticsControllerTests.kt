package com.northwind.resolve.analytics.api

import com.northwind.resolve.analytics.application.AnalyticsService
import com.northwind.resolve.common.api.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate

@WebMvcTest(AnalyticsController::class)
@ActiveProfiles("database")
@Import(ApiExceptionHandler::class)
class AnalyticsControllerTests {
 @Autowired lateinit var mockMvc: MockMvc
 @MockitoBean lateinit var service: AnalyticsService
 @Test fun `dashboard returns contract response`() { val period=AnalyticsPeriod(LocalDate.parse("2024-01-01"),LocalDate.parse("2024-12-01")); val response=DashboardKpisResponse(Instant.EPOCH,period,DashboardMetrics(1,null,null,null,null,0,null),emptyList(),emptyList(),emptyMap()); `when`(service.dashboard(null,null,null)).thenReturn(response); mockMvc.perform(get("/api/dashboard/kpis")).andExpect(status().isOk).andExpect(jsonPath("$.period.from").value("2024-01-01")).andExpect(jsonPath("$.metrics.backlog").value(1)) }
 @Test fun `value case returns contract response`() { val p=AnalyticsPeriod(LocalDate.parse("2024-01-01"),LocalDate.parse("2024-12-01")); val response=ValueCaseResponse(p,ValueCaseAssumptions(null,null,null,null),ValueCaseObservedBaseline(0,0,null,null,0),null,null,null,null,null,null,emptyMap()); `when`(service.valueCase(null,null,null,null)).thenReturn(response); mockMvc.perform(get("/api/value-case")).andExpect(status().isOk).andExpect(jsonPath("$.observedBaseline.annualComplaints").value(0)).andExpect(jsonPath("$.period.to").value("2024-12-01")) }
}
