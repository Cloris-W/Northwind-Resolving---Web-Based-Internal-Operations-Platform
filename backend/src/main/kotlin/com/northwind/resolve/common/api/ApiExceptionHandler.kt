package com.northwind.resolve.common.api

import com.northwind.resolve.cases.application.AccountNotFoundException
import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.InvalidRequestException
import com.northwind.resolve.cases.application.InvalidCaseTransferException
import com.northwind.resolve.cases.application.MutationConflictException
import com.northwind.resolve.billing.application.BillingExceptionNotFoundException
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

    @ExceptionHandler(InvalidCaseTransferException::class)
    fun invalidTransfer(error: InvalidCaseTransferException, request: HttpServletRequest) = error(HttpStatus.BAD_REQUEST, "INVALID_CASE_TRANSFER", error.message ?: "Invalid case transfer", request)

    @ExceptionHandler(MutationConflictException::class)
    fun mutationConflict(error: MutationConflictException, request: HttpServletRequest) = error(HttpStatus.CONFLICT, "STATE_CONFLICT", error.message ?: "Mutation conflict", request)

    @ExceptionHandler(BillingExceptionNotFoundException::class)
    fun billingExceptionNotFound(error: BillingExceptionNotFoundException, request: HttpServletRequest) = error(HttpStatus.NOT_FOUND, "BILLING_EXCEPTION_NOT_FOUND", error.message!!, request)

    private fun error(status: HttpStatus, code: String, message: String, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(Instant.now(), status.value(), code, message, request.getHeader("X-Trace-Id") ?: UUID.randomUUID().toString()))
}
