package com.northwind.resolve.importing

import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus

/** Explicit Phase 2 mappings for the values supplied by northwind_complaints.csv. */
object ComplaintNormalization {
    private val priorities = mapOf(
        "P1" to CasePriority.CRITICAL,
        "P2" to CasePriority.HIGH,
        "P3" to CasePriority.MEDIUM,
    )

    private val categories = mapOf(
        "Billing - disputed amount" to CaseCategory.BILLING,
        "Billing - estimated read" to CaseCategory.BILLING,
        "Metering - no read taken" to CaseCategory.METERING,
        "Payment - plan or arrears" to CaseCategory.BILLING,
        "Service - missed appointment" to CaseCategory.SERVICE,
        "Service - poor communication" to CaseCategory.SERVICE,
        "Supply - interruption" to CaseCategory.SERVICE,
        "Water - pressure or quality" to CaseCategory.SERVICE,
        "Other" to CaseCategory.OTHER,
    )

    private val statuses = mapOf(
        "Open" to CaseStatus.OPEN,
        "Closed" to CaseStatus.CLOSED,
        "Closed - reopened" to CaseStatus.IN_PROGRESS,
    )

    fun priority(rawValue: String): CasePriority = priorities[rawValue]
        ?: error("Unsupported complaint priority: $rawValue")

    fun category(rawValue: String): CaseCategory = categories[rawValue]
        ?: error("Unsupported complaint category: $rawValue")

    fun status(rawValue: String): CaseStatus = statuses[rawValue]
        ?: error("Unsupported complaint status: $rawValue")
}
