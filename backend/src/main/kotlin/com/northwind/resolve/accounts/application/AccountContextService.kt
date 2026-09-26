package com.northwind.resolve.accounts.application

import com.northwind.resolve.billing.api.AccountBillingHistoryResponse
import com.northwind.resolve.billing.api.BillCorrectionDto
import com.northwind.resolve.billing.persistence.BillingHistoryReadRepository
import com.northwind.resolve.cases.application.AccountNotFoundException
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.cases.application.InvalidRequestException
import com.northwind.resolve.metering.api.MeterReadingDto
import com.northwind.resolve.metering.api.MeterReadingsResponse
import com.northwind.resolve.metering.persistence.MeterReadingReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@Profile("database")
class AccountContextService(
    private val caseWorkspace: CaseWorkspaceService,
    private val meterReadings: MeterReadingReadRepository,
    private val billing: BillingHistoryReadRepository,
) {
    fun meterHistory(accountId: String, from: Instant?, to: Instant?): MeterReadingsResponse {
        validateRange(from, to)
        requireKnownAccount(accountId)
        return MeterReadingsResponse(accountId, meterReadings.findByAccount(accountId, from, to).map { MeterReadingDto(it.accountId, it.meterId, it.readingAt, it.value, it.estimated, it.region) })
    }

    fun billingHistory(accountId: String, from: Instant?, to: Instant?): AccountBillingHistoryResponse {
        validateRange(from, to)
        requireKnownAccount(accountId)
        // Challenge data contains neither invoices nor canonical correction records, so bills remain truthfully empty.
        return AccountBillingHistoryResponse(accountId, emptyList(), billing.findCorrections(accountId, from, to).map { BillCorrectionDto(it.id, it.accountId, it.originalValue, it.correctedValue, it.reason, it.region, it.createdAt) })
    }

    private fun requireKnownAccount(accountId: String) { if (!caseWorkspace.accountExists(accountId)) throw AccountNotFoundException(accountId) }
    private fun validateRange(from: Instant?, to: Instant?) { if (from != null && to != null && from > to) throw InvalidRequestException("from must be before or equal to to") }
}
