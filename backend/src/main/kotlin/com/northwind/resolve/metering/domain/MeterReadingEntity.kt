package com.northwind.resolve.metering.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "meter_readings")
class MeterReadingEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "account_id", nullable = false, length = 64) var accountId: String = "",
    @Column(name = "meter_id", nullable = false, length = 64) var meterId: String = "",
    @Column(name = "reading_at", nullable = false) var readingAt: Instant = Instant.EPOCH,
    @Column(nullable = false, precision = 18, scale = 4) var value: BigDecimal = BigDecimal.ZERO,
    @Column(name = "estimated_flag", nullable = false) var estimated: Boolean = false,
    @Column(nullable = false, length = 64) var region: String = ""
)
