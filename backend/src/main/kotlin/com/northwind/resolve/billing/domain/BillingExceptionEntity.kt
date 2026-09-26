package com.northwind.resolve.billing.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "billing_exceptions")
class BillingExceptionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "account_id", nullable = false, length = 64) var accountId: String = "",
    @Column(name = "case_id", length = 64) var caseId: String? = null,
    @Column(name = "risk_score", nullable = false) var riskScore: Int = 0,
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false, length = 16) var riskLevel: RiskLevel = RiskLevel.LOW,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "reason_codes", nullable = false, columnDefinition = "jsonb") var reasonCodes: String = "[]",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) var status: BillingExceptionStatus = BillingExceptionStatus.OPEN,
    @Column(nullable = false, length = 64) var region: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.EPOCH,
    @Column(name = "reviewed_by", length = 128) var reviewedBy: String? = null
)
