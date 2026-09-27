package com.northwind.resolve.fieldforce.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.cases.application.CaseNotFoundException
import com.northwind.resolve.cases.application.InvalidRequestException
import com.northwind.resolve.cases.application.MutationConflictException
import com.northwind.resolve.cases.application.toDto
import com.northwind.resolve.cases.domain.CaseEventEntity
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.SourceSystem
import com.northwind.resolve.cases.persistence.CaseMutationRepository
import com.northwind.resolve.cases.persistence.CaseWorkspaceRepository
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import com.northwind.resolve.fieldforce.api.FieldVisitRequest
import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import com.northwind.resolve.integrations.FieldForceProvider
import com.northwind.resolve.integrations.FieldForceRequestContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
@Profile("database")
class FieldVisitService(
    private val workspace: CaseWorkspaceRepository,
    private val mutations: CaseMutationRepository,
    private val fieldForce: FieldForceProvider,
    private val idempotency: MutationIdempotencyRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun request(caseId: String, request: FieldVisitRequest, key: String): FieldVisitDto {
        validate(request, key)
        val fingerprint = fingerprint(caseId, request.requestedFor.toString(), request.visitReason, request.meterId, request.instructions)
        idempotency.lock(OPERATION, key)
        idempotency.find(OPERATION, key)?.let { stored ->
            if (stored.fingerprint != fingerprint) throw MutationConflictException("Idempotency key was already used for a different field-visit request")
            return objectMapper.readValue(stored.responseJson, FieldVisitDto::class.java)
        }
        val case = workspace.findCase(caseId) ?: throw CaseNotFoundException(caseId)
        val visit = fieldForce.requestVisit(
            FieldForceRequestContext(caseId, case.region, request.requestedFor, request.visitReason, request.meterId, request.instructions),
            UUID.randomUUID(),
        )
        val response = FieldVisitDto(visit.visitId, visit.caseId, FieldVisitStatus.valueOf(visit.status), visit.scheduledAt, visit.completedAt, visit.outcome)
        mutations.append(CaseEventEntity(
            eventId = UUID.randomUUID(), caseId = caseId, eventType = CaseEventType.FIELD_VISIT_REQUESTED,
            sourceSystem = SourceSystem.FIELDFORCE, occurredAt = Instant.now(), actor = SYSTEM_ACTOR,
            description = "Field visit requested", metadataJson = objectMapper.writeValueAsString(mapOf("actorType" to "SYSTEM_GENERATED", "visitId" to visit.visitId.toString())),
        ))
        idempotency.store(OPERATION, key, fingerprint, objectMapper.writeValueAsString(response))
        return response
    }

    private fun validate(request: FieldVisitRequest, key: String) {
        if (key.length !in 8..128) throw InvalidRequestException("Idempotency-Key must be between 8 and 128 characters")
        if (request.visitReason.isBlank() || request.visitReason.length > 500) throw InvalidRequestException("visitReason must be between 1 and 500 characters")
        if (request.meterId?.length ?: 0 > 64) throw InvalidRequestException("meterId must not exceed 64 characters")
        if (request.instructions?.length ?: 0 > 500) throw InvalidRequestException("instructions must not exceed 500 characters")
    }

    companion object { private const val OPERATION = "FIELD_VISIT_REQUEST"; private const val SYSTEM_ACTOR = "SYSTEM_GENERATED" }
}

private fun fingerprint(vararg values: String?): String = MessageDigest.getInstance("SHA-256")
    .digest(values.joinToString("\u001f") { it ?: "<null>" }.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
