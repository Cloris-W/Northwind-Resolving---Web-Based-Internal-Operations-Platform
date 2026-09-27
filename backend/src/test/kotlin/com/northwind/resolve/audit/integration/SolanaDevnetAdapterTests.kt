package com.northwind.resolve.audit.integration
import com.northwind.resolve.audit.application.AuditHashService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class SolanaDevnetAdapterTests { @Test fun `memo program and hash-only memo are exact`(){val hash="a".repeat(64);val memo=AuditHashService().memo(hash);assertEquals("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr",SolanaDevnetAdapter.MEMO_PROGRAM_ID);assertEquals(98,memo.toByteArray().size);assertEquals("northwind-resolve:audit:v1:sha256:$hash",memo);assertFalse(memo.contains("case"))}}
