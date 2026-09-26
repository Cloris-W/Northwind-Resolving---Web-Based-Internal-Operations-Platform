package com.northwind.resolve.billing.domain

enum class BillingExceptionStatus { OPEN, IN_REVIEW, RESOLVED, DISMISSED }
enum class RiskLevel { LOW, MEDIUM, HIGH }
enum class BillingReviewAction { VERIFY_READING, REQUEST_FIELD_VISIT, CORRECT_BILL, APPROVE }
