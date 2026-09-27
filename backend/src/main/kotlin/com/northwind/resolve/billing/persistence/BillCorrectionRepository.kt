package com.northwind.resolve.billing.persistence

import com.northwind.resolve.billing.domain.BillCorrectionEntity
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

@Repository @Profile("database")
class BillCorrectionRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun existsEarlier(accountId: String, evaluatedAt: Instant): Boolean = (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM bill_corrections WHERE account_id=:accountId AND created_at < :evaluatedAt)",mapOf("accountId" to accountId,"evaluatedAt" to evaluatedAt.atOffset(ZoneOffset.UTC)),Boolean::class.java) ?: false)
    fun create(correction: BillCorrectionEntity) { jdbc.update("INSERT INTO bill_corrections (id,account_id,original_value,corrected_value,reason,region,created_at) VALUES (:id,:accountId,:originalValue,:correctedValue,:reason,:region,:createdAt)",mapOf("id" to correction.id,"accountId" to correction.accountId,"originalValue" to correction.originalValue,"correctedValue" to correction.correctedValue,"reason" to correction.reason,"region" to correction.region,"createdAt" to correction.createdAt.atOffset(ZoneOffset.UTC))) }
}
