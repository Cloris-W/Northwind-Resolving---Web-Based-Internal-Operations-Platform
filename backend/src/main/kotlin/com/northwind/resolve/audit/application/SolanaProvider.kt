package com.northwind.resolve.audit.application

data class SolanaSubmission(val signature: String)
enum class SolanaAnchorVerification { MATCH, MISMATCH, PENDING }
interface SolanaProvider { fun submit(memo: String): SolanaSubmission; fun verify(signature: String, expectedMemo: String): SolanaAnchorVerification }
