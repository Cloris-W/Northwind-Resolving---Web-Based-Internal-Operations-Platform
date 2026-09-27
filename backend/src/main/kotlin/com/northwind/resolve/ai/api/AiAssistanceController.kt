package com.northwind.resolve.ai.api

import com.northwind.resolve.ai.application.AiAssistanceService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("database")
@RequestMapping("/api/ai/cases/{caseId}")
class AiAssistanceController(private val assistance: AiAssistanceService) {
    @PostMapping("/summary") fun summary(@PathVariable caseId: String) = assistance.summary(caseId)
    @PostMapping("/recommendation") fun recommendation(@PathVariable caseId: String) = assistance.recommendation(caseId)
}
