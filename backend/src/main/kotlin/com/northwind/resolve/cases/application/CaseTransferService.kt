package com.northwind.resolve.cases.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.cases.api.CaseDto
import com.northwind.resolve.cases.api.CaseEventDto
import com.northwind.resolve.cases.api.TransferCaseRequest
import com.northwind.resolve.cases.api.TransferCaseResponse
import com.northwind.resolve.cases.domain.CaseEventEntity
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.SourceSystem
import com.northwind.resolve.cases.persistence.CaseMutationRepository
import com.northwind.resolve.cases.persistence.CaseWorkspaceRepository
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
@Profile("database")
class CaseTransferService(
    private val workspace: CaseWorkspaceRepository,
    private val mutations: CaseMutationRepository,
    private val idempotency: MutationIdempotencyRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun transfer(caseId: String, request: TransferCaseRequest, key: String): TransferCaseResponse {
        validate(request, key)
        val fingerprint = fingerprint(caseId, request.assignedTeam, request.reason)
        idempotency.lock(OPERATION, key)
        idempotency.find(OPERATION, key)?.let { stored ->
            if (stored.fingerprint != fingerprint) throw MutationConflictException("Idempotency key was already used for a different transfer")
            return objectMapper.readValue(stored.responseJson, TransferCaseResponse::class.java)
        }
        val existing = workspace.findCase(caseId) ?: throw CaseNotFoundException(caseId)
        if (!mutations.updateAssignment(caseId, request.assignedTeam)) throw CaseNotFoundException(caseId)
        val event = CaseEventEntity(
            eventId = UUID.randomUUID(), caseId = caseId, eventType = CaseEventType.TRANSFERRED,
            sourceSystem = SourceSystem.RESOLVE, occurredAt = Instant.now(), actor = SYSTEM_ACTOR,
            description = "Case transferred to ${request.assignedTeam}",
            metadataJson = objectMapper.writeValueAsString(mapOf("actorType" to "SYSTEM_GENERATED", "reason" to request.reason)),
        )
        mutations.append(event)
        val response = TransferCaseResponse(
            existing.toDto(request.assignedTeam),
            event.toDto(objectMapper),
        )
        idempotency.store(OPERATION, key, fingerprint, objectMapper.writeValueAsString(response))
        return response
    }

    private fun validate(request: TransferCaseRequest, key: String) {
        if (key.length !in 8..128) throw InvalidCaseTransferException("Idempotency-Key must be between 8 and 128 characters")
        if (request.assignedTeam.isBlank() || request.assignedTeam.length > 100) throw InvalidCaseTransferException("assignedTeam must be between 1 and 100 characters")
        if (request.reason != null && request.reason.length > 500) throw InvalidCaseTransferException("reason must not exceed 500 characters")
    }

    companion object { private const val OPERATION = "CASE_TRANSFER"; private const val SYSTEM_ACTOR = "SYSTEM_GENERATED" }
}

internal fun com.northwind.resolve.cases.domain.CaseEntity.toDto(assignedTeam: String = this.assignedTeam) =
    CaseDto(caseId, accountId, category, priority, region, status, slaDays, openedAt, closedAt, assignedTeam)

internal fun CaseEventEntity.toDto(objectMapper: ObjectMapper) = CaseEventDto(
    eventId, caseId, eventType, sourceSystem, occurredAt, actor, description,
    metadataJson?.let { objectMapper.readValue(it, Map::class.java) as Map<String, Any?> },
)

private fun fingerprint(vararg values: String?): String = MessageDigest.getInstance("SHA-256")
    .digest(values.joinToString("\u001f") { it ?: "<null>" }.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
