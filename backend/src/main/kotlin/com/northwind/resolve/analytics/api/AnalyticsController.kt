package com.northwind.resolve.analytics.api

import com.northwind.resolve.analytics.application.AnalyticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.time.LocalDate

@RestController @Profile("database") @RequestMapping("/api") class AnalyticsController(private val service: AnalyticsService) {
 @GetMapping("/dashboard/kpis") fun dashboard(@RequestParam(required=false) region:String?,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) from:LocalDate?,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) to:LocalDate?)=service.dashboard(region,from,to)
 @GetMapping("/value-case") fun valueCase(@RequestParam(required=false) complaintReductionPercent:BigDecimal?,@RequestParam(required=false) transferReductionPercent:BigDecimal?,@RequestParam(required=false) annualOperatingCost:BigDecimal?,@RequestParam(required=false) implementationCost:BigDecimal?):ValueCaseResponse { require(complaintReductionPercent==null||complaintReductionPercent in BigDecimal.ZERO..BigDecimal(100)){"complaintReductionPercent must be between 0 and 100"};require(transferReductionPercent==null||transferReductionPercent in BigDecimal.ZERO..BigDecimal(100)){"transferReductionPercent must be between 0 and 100"};require(annualOperatingCost==null||annualOperatingCost>=BigDecimal.ZERO){"annualOperatingCost must be non-negative"};require(implementationCost==null||implementationCost>=BigDecimal.ZERO){"implementationCost must be non-negative"};return service.valueCase(complaintReductionPercent,transferReductionPercent,annualOperatingCost,implementationCost) }
}
