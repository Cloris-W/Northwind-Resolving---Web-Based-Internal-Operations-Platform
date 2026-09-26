package com.northwind.resolve.billing.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "bill_corrections")
class BillCorrectionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "account_id", nullable = false, length = 64) var accountId: String = "",
    @Column(name = "original_value", nullable = false, precision = 18, scale = 4) var originalValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "corrected_value", nullable = false, precision = 18, scale = 4) var correctedValue: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 500) var reason: String = "",
    @Column(nullable = false, length = 64) var region: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.EPOCH
)
