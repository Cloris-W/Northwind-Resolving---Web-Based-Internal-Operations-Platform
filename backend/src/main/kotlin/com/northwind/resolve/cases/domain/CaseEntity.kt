package com.northwind.resolve.cases.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "cases")
class CaseEntity(
    @Id
    @Column(name = "case_id", length = 64)
    var caseId: String = "",
    @Column(name = "account_id", nullable = false, length = 64)
    var accountId: String = "",
    @Column(name = "source_system_id", length = 64)
    var sourceSystemId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var category: CaseCategory = CaseCategory.OTHER,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var priority: CasePriority = CasePriority.MEDIUM,
    @Column(nullable = false, length = 64)
    var region: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: CaseStatus = CaseStatus.OPEN,
    @Column(name = "sla_days", nullable = false)
    var slaDays: Int = 0,
    @Column(name = "opened_at", nullable = false)
    var openedAt: Instant = Instant.EPOCH,
    @Column(name = "closed_at")
    var closedAt: Instant? = null,
    @Column(name = "assigned_team", nullable = false, length = 100)
    var assignedTeam: String = ""
)
