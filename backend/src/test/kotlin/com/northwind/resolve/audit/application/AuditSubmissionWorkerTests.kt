package com.northwind.resolve.audit.application
import com.northwind.resolve.audit.config.SolanaProperties
import com.northwind.resolve.audit.domain.*
import com.northwind.resolve.audit.persistence.AuditEventRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.util.UUID
class AuditSubmissionWorkerTests { private val repo=mock(AuditEventRepository::class.java);private val provider=mock(SolanaProvider::class.java);private val props=SolanaProperties(enabled=true,cluster="devnet",keypairPath="test.json");private val audit=AuditEventEntity(UUID.randomUUID(),"case",AuditEventType.BILL_CORRECTED,"a".repeat(64),null,"source",AuditStatus.PENDING,Instant.EPOCH)
 @Test fun `success marks submitted`(){`when`(repo.pending(20)).thenReturn(listOf(audit));`when`(repo.find(audit.id)).thenReturn(audit);`when`(provider.submit(anyString())).thenReturn(SolanaSubmission("1".repeat(64)));AuditSubmissionWorker(repo,provider,props).submitPending();assertTrue(mockingDetails(repo).invocations.any{it.method.name=="submitted"})}
 @Test fun `provider failure remains pending`(){`when`(repo.pending(20)).thenReturn(listOf(audit));`when`(repo.find(audit.id)).thenReturn(audit);`when`(provider.submit(anyString())).thenThrow(IllegalStateException());AuditSubmissionWorker(repo,provider,props).submitPending();assertTrue(mockingDetails(repo).invocations.none{it.method.name=="submitted"})}}
