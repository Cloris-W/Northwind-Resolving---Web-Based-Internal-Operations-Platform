package com.northwind.resolve.billing.persistence

import com.northwind.resolve.billing.domain.*
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

data class BillingExceptionPage(val items: List<BillingExceptionEntity>, val total: Long)
@Repository @Profile("database")
class BillingExceptionRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun find(id: UUID): BillingExceptionEntity? = jdbc.query("SELECT * FROM billing_exceptions WHERE id=:id", mapOf("id" to id)) { rs, _ -> map(rs) }.firstOrNull()
    fun existsForCase(caseId: String) = (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM billing_exceptions WHERE case_id=:caseId)", mapOf("caseId" to caseId), Boolean::class.java) ?: false)
    fun create(entity: BillingExceptionEntity) { jdbc.update("""INSERT INTO billing_exceptions (id,account_id,case_id,risk_score,risk_level,reason_codes,status,region,created_at,reviewed_by) VALUES (:id,:accountId,:caseId,:riskScore,:riskLevel,CAST(:reasonCodes AS jsonb),:status,:region,:createdAt,:reviewedBy)""", mapOf("id" to entity.id,"accountId" to entity.accountId,"caseId" to entity.caseId,"riskScore" to entity.riskScore,"riskLevel" to entity.riskLevel.name,"reasonCodes" to entity.reasonCodes,"status" to entity.status.name,"region" to entity.region,"createdAt" to entity.createdAt.atOffset(ZoneOffset.UTC),"reviewedBy" to entity.reviewedBy)) }
    fun updateStatus(id: UUID, status: BillingExceptionStatus, reviewedBy: String) { jdbc.update("UPDATE billing_exceptions SET status=:status, reviewed_by=:reviewedBy WHERE id=:id", mapOf("id" to id,"status" to status.name,"reviewedBy" to reviewedBy)) }
    fun page(risk: RiskLevel?, status: BillingExceptionStatus?, region: String?, page: Int, size: Int): BillingExceptionPage { val where=mutableListOf<String>(); val p=mutableMapOf<String,Any?>(); risk?.let{where+="risk_level=:risk";p["risk"]=it.name};status?.let{where+="status=:status";p["status"]=it.name};region?.takeIf{it.isNotBlank()}?.let{where+="region=:region";p["region"]=it}; val clause=if(where.isEmpty())"" else " WHERE ${where.joinToString(" AND ")}"; val total=jdbc.queryForObject("SELECT COUNT(*) FROM billing_exceptions$clause",p,Long::class.java)?:0; p["limit"]=size;p["offset"]=page*size; return BillingExceptionPage(jdbc.query("SELECT * FROM billing_exceptions$clause ORDER BY created_at DESC,id DESC LIMIT :limit OFFSET :offset",p){rs,_->map(rs)},total) }
    private fun map(rs: java.sql.ResultSet)=BillingExceptionEntity(rs.getObject("id",UUID::class.java),rs.getString("account_id"),rs.getString("case_id"),rs.getInt("risk_score"),RiskLevel.valueOf(rs.getString("risk_level")),rs.getString("reason_codes"),BillingExceptionStatus.valueOf(rs.getString("status")),rs.getString("region"),rs.getObject("created_at",java.time.OffsetDateTime::class.java).toInstant(),rs.getString("reviewed_by"))
}
