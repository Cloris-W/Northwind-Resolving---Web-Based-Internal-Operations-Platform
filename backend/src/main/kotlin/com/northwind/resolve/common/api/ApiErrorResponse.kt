package com.northwind.resolve.common.api

import java.time.Instant

data class ApiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val traceId: String
)

data class ValidationErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val traceId: String,
    val violations: List<FieldViolation>
)

data class FieldViolation(val field: String, val message: String)
