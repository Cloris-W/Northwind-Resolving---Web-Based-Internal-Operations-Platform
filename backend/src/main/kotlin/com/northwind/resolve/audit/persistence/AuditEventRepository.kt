package com.northwind.resolve.audit.persistence

import com.northwind.resolve.audit.domain.*
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository @Profile("database")
class AuditEventRepository(private val jdbc:NamedParameterJdbcTemplate) {
 fun create(e:AuditEventEntity){jdbc.update("INSERT INTO audit_events (id,case_id,event_type,payload_hash,solana_signature,source_event_reference,status,created_at) VALUES (:id,:caseId,:type,:hash,:sig,:ref,:status,:at)",mapOf("id" to e.id,"caseId" to e.caseId,"type" to e.eventType.name,"hash" to e.payloadHash,"sig" to e.solanaSignature,"ref" to e.sourceEventReference,"status" to e.status.name,"at" to e.createdAt.atOffset(ZoneOffset.UTC)))}
 fun find(id:UUID)=jdbc.query("SELECT * FROM audit_events WHERE id=:id",mapOf("id" to id)){rs,_->row(rs)}.firstOrNull()
 fun findBySource(type:AuditEventType,ref:String)=jdbc.query("SELECT * FROM audit_events WHERE event_type=:type AND source_event_reference=:ref",mapOf("type" to type.name,"ref" to ref)){rs,_->row(rs)}.firstOrNull()
 fun list(caseId:String)=jdbc.query("SELECT * FROM audit_events WHERE case_id=:caseId ORDER BY created_at DESC,id DESC",mapOf("caseId" to caseId)){rs,_->row(rs)}
 fun pending(limit:Int)=jdbc.query("SELECT * FROM audit_events WHERE status='PENDING' ORDER BY created_at ASC,id ASC LIMIT :limit",mapOf("limit" to limit)){rs,_->row(rs)}
 fun submitted(id:UUID,signature:String){jdbc.update("UPDATE audit_events SET solana_signature=:signature,status='SUBMITTED' WHERE id=:id AND status='PENDING'",mapOf("id" to id,"signature" to signature))}
 fun status(id:UUID,status:AuditStatus){jdbc.update("UPDATE audit_events SET status=:status WHERE id=:id",mapOf("id" to id,"status" to status.name))}
 private fun row(rs:java.sql.ResultSet)=AuditEventEntity(rs.getObject("id",UUID::class.java),rs.getString("case_id"),AuditEventType.valueOf(rs.getString("event_type")),rs.getString("payload_hash"),rs.getString("solana_signature"),rs.getString("source_event_reference"),AuditStatus.valueOf(rs.getString("status")),rs.getObject("created_at",OffsetDateTime::class.java).toInstant())
}
