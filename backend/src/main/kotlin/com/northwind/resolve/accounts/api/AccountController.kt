package com.northwind.resolve.accounts.api

import com.northwind.resolve.accounts.application.AccountContextService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@Profile("database")
@RequestMapping("/api/accounts")
class AccountController(private val accounts: AccountContextService) {
    @GetMapping("/{accountId}/meter-readings")
    fun meterReadings(@PathVariable accountId: String, @RequestParam(required = false) from: Instant?, @RequestParam(required = false) to: Instant?) = accounts.meterHistory(accountId, from, to)

    @GetMapping("/{accountId}/billing")
    fun billingHistory(@PathVariable accountId: String, @RequestParam(required = false) from: Instant?, @RequestParam(required = false) to: Instant?) = accounts.billingHistory(accountId, from, to)
}
