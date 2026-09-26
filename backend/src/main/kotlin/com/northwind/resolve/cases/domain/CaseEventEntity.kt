package com.northwind.resolve.cases.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "case_events")
class CaseEventEntity(
    @Id
    @Column(name = "event_id")
    var eventId: UUID = UUID.randomUUID(),
    @Column(name = "case_id", nullable = false, length = 64)
    var caseId: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    var eventType: CaseEventType = CaseEventType.COMPLAINT_CREATED,
    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 32)
    var sourceSystem: SourceSystem = SourceSystem.RESOLVE,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.EPOCH,
    @Column(nullable = false, length = 128)
    var actor: String = "",
    @Column(nullable = false, length = 1000)
    var description: String = "",
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    var metadataJson: String? = null
)
