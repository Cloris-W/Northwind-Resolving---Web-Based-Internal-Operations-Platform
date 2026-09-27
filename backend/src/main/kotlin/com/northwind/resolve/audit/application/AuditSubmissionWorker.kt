package com.northwind.resolve.audit.application
import com.northwind.resolve.audit.config.SolanaProperties
import com.northwind.resolve.audit.domain.AuditStatus
import com.northwind.resolve.audit.persistence.AuditEventRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
@Component class AuditSubmissionWorker(private val audits:AuditEventRepository,private val solana:SolanaProvider,private val properties:SolanaProperties){private val hashes=AuditHashService();@Scheduled(fixedDelayString="\${'$'}{northwind.solana.retry-delay:PT5M}") fun submitPending(){if(!properties.enabled||properties.cluster!="devnet"||properties.keypairPath.isBlank())return;audits.pending(20).forEach{candidate->val audit=audits.find(candidate.id)?:return@forEach;if(audit.status!=AuditStatus.PENDING)return@forEach;runCatching{solana.submit(hashes.memo(audit.payloadHash))}.onSuccess{audits.submitted(audit.id,it.signature)}}}}
