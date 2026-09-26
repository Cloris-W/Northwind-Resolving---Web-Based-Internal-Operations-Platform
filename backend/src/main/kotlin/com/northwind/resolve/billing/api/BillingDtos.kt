package com.northwind.resolve.billing.api

import com.northwind.resolve.billing.domain.BillingExceptionStatus
import com.northwind.resolve.billing.domain.BillingReviewAction
import com.northwind.resolve.billing.domain.RiskLevel
import com.northwind.resolve.common.api.PageMetadata
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class BillingRecordDto(val billingId: String, val accountId: String, val periodStart: LocalDate, val periodEnd: LocalDate, val amount: BigDecimal, val status: BillingRecordStatus)
enum class BillingRecordStatus { ISSUED, CORRECTED, VOIDED }
data class BillCorrectionDto(val id: UUID, val accountId: String, val originalValue: BigDecimal, val correctedValue: BigDecimal, val reason: String, val region: String, val createdAt: Instant)
data class AccountBillingHistoryResponse(val accountId: String, val bills: List<BillingRecordDto>, val corrections: List<BillCorrectionDto>)
data class BillingExceptionDto(val id: UUID, val accountId: String, val caseId: String?, val riskScore: Int, val riskLevel: RiskLevel, val reasonCodes: List<String>, val status: BillingExceptionStatus, val region: String, val createdAt: Instant, val reviewedBy: String?)
data class BillingExceptionListResponse(val items: List<BillingExceptionDto>, val page: PageMetadata)

sealed interface BillingExceptionReviewRequest { val action: BillingReviewAction; val reviewedBy: String; val notes: String? }
data class VerifyReadingReviewRequest(override val reviewedBy: String, override val notes: String? = null) : BillingExceptionReviewRequest { override val action = BillingReviewAction.VERIFY_READING }
data class RequestFieldVisitReviewRequest(override val reviewedBy: String, override val notes: String? = null) : BillingExceptionReviewRequest { override val action = BillingReviewAction.REQUEST_FIELD_VISIT }
data class CorrectBillReviewRequest(override val reviewedBy: String, val correction: BillCorrectionDraft, override val notes: String? = null) : BillingExceptionReviewRequest { override val action = BillingReviewAction.CORRECT_BILL }
data class ApproveReviewRequest(override val reviewedBy: String, override val notes: String? = null) : BillingExceptionReviewRequest { override val action = BillingReviewAction.APPROVE }
data class BillCorrectionDraft(val originalValue: BigDecimal, val correctedValue: BigDecimal, val reason: String, val region: String)
data class BillingExceptionReviewResponse(val exception: BillingExceptionDto, val action: BillingReviewAction, val correction: BillCorrectionDto? = null, val fieldVisit: FieldVisitDto? = null, val reviewedAt: Instant)
