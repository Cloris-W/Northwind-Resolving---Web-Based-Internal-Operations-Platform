package com.northwind.resolve.billing.application

import com.northwind.resolve.billing.domain.BillingExceptionEntity
import com.northwind.resolve.billing.persistence.BillingHistoryReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/** Billing-owned, minimized read boundary for advisory AI use cases. */
data class BillingAiContext(val latestException: BillingExceptionEntity?, val correctionCount: Long)

@Service
@Profile("database")
class BillingAiContextService(private val history: BillingHistoryReadRepository) {
    fun forCase(caseId: String, accountId: String): BillingAiContext =
        BillingAiContext(history.findLatestException(caseId), history.correctionCount(accountId))
}
