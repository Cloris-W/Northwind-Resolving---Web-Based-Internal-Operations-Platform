package com.northwind.resolve.common.persistence

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

data class StoredMutation(val fingerprint: String, val responseJson: String)

@Repository
@Profile("database")
class MutationIdempotencyRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun lock(operation: String, key: String) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtext(:lockKey))", mapOf("lockKey" to "$operation:$key"), Any::class.java)
    }

    fun find(operation: String, key: String): StoredMutation? = jdbc.query(
        "SELECT request_fingerprint, response_json::text FROM mutation_idempotency WHERE operation = :operation AND idempotency_key = :key",
        mapOf("operation" to operation, "key" to key),
    ) { rs, _ -> StoredMutation(rs.getString(1), rs.getString(2)) }.firstOrNull()

    fun store(operation: String, key: String, fingerprint: String, responseJson: String) {
        jdbc.update(
            """INSERT INTO mutation_idempotency (operation, idempotency_key, request_fingerprint, response_json, created_at)
               VALUES (:operation, :key, :fingerprint, CAST(:responseJson AS jsonb), :createdAt)""",
            mapOf("operation" to operation, "key" to key, "fingerprint" to fingerprint, "responseJson" to responseJson, "createdAt" to Instant.now().atOffset(ZoneOffset.UTC)),
        )
    }
}
