package com.northwind.resolve.common.api

import com.northwind.resolve.cases.application.AccountNotFoundException
import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.InvalidRequestException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
@Profile("database")
class ApiExceptionHandler {
    @ExceptionHandler(CaseNotFoundException::class)
    fun caseNotFound(error: CaseNotFoundException, request: HttpServletRequest) = error(HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", error.message!!, request)

    @ExceptionHandler(AccountNotFoundException::class)
    fun accountNotFound(error: AccountNotFoundException, request: HttpServletRequest) = error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", error.message!!, request)

    @ExceptionHandler(InvalidRequestException::class, IllegalArgumentException::class)
    fun invalidRequest(error: RuntimeException, request: HttpServletRequest) = error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.message ?: "Invalid request", request)

    private fun error(status: HttpStatus, code: String, message: String, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(Instant.now(), status.value(), code, message, request.getHeader("X-Trace-Id") ?: UUID.randomUUID().toString()))
}
