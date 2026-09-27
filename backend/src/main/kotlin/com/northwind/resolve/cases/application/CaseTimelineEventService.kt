package com.northwind.resolve.cases.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.cases.domain.CaseEventEntity
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.SourceSystem
import com.northwind.resolve.cases.persistence.CaseMutationRepository
import com.northwind.resolve.cases.persistence.CaseWorkspaceRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@Profile("database")
class CaseTimelineEventService(private val cases: CaseWorkspaceRepository, private val mutations: CaseMutationRepository, private val objectMapper: ObjectMapper) {
    fun exists(caseId: String?) = caseId != null && cases.findCase(caseId) != null
    fun appendIfCaseExists(caseId: String?, type: CaseEventType, actor: String, description: String, metadata: Map<String, Any?> = emptyMap()): Boolean {
        if (caseId == null || cases.findCase(caseId) == null) return false
        mutations.append(CaseEventEntity(UUID.randomUUID(), caseId, type, SourceSystem.BILLING, Instant.now(), actor, description, objectMapper.writeValueAsString(metadata)))
        return true
    }
}
