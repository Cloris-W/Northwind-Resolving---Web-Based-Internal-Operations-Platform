package com.northwind.resolve.accounts.api

import com.northwind.resolve.accounts.application.AccountContextService
import com.northwind.resolve.billing.api.AccountBillingHistoryResponse
import com.northwind.resolve.cases.application.AccountNotFoundException
import com.northwind.resolve.common.api.ApiExceptionHandler
import com.northwind.resolve.metering.api.MeterReadingsResponse
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

@WebMvcTest(AccountController::class)
@ActiveProfiles("database")
@Import(ApiExceptionHandler::class)
class AccountControllerTests {
    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var accounts: AccountContextService

    @Test fun `returns truthful empty meter and billing arrays for a known account`() {
        `when`(accounts.meterHistory("ACC-943644", null, null)).thenReturn(MeterReadingsResponse("ACC-943644", emptyList()))
        `when`(accounts.billingHistory("ACC-943644", null, null)).thenReturn(AccountBillingHistoryResponse("ACC-943644", emptyList(), emptyList()))
        mockMvc.perform(get("/api/accounts/ACC-943644/meter-readings")).andExpect(status().isOk).andExpect(jsonPath("$.readings").isEmpty)
        mockMvc.perform(get("/api/accounts/ACC-943644/billing")).andExpect(status().isOk).andExpect(jsonPath("$.bills").isEmpty).andExpect(jsonPath("$.corrections").isEmpty)
    }

    @Test fun `returns account not found`() {
        `when`(accounts.meterHistory("missing", null, null)).thenThrow(AccountNotFoundException("missing"))
        mockMvc.perform(get("/api/accounts/missing/meter-readings")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
    }
}
