package com.northwind.resolve.metering.api

import java.math.BigDecimal
import java.time.Instant

data class MeterReadingDto(val accountId: String, val meterId: String, val timestamp: Instant, val value: BigDecimal, val estimated: Boolean, val region: String)
data class MeterReadingsResponse(val accountId: String, val readings: List<MeterReadingDto>)
