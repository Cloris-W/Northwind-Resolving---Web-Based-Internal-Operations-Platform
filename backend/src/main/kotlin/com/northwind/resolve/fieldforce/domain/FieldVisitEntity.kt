package com.northwind.resolve.fieldforce.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class FieldVisitStatus { REQUESTED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }

@Entity
@Table(name = "field_visits")
class FieldVisitEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "case_id", nullable = false, length = 64) var caseId: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) var status: FieldVisitStatus = FieldVisitStatus.REQUESTED,
    @Column(name = "scheduled_at", nullable = false) var scheduledAt: Instant = Instant.EPOCH,
    @Column(name = "completed_at") var completedAt: Instant? = null,
    @Column(length = 1000) var outcome: String? = null
)
