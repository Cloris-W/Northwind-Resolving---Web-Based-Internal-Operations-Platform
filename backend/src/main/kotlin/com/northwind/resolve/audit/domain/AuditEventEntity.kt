package com.northwind.resolve.audit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class AuditEventType { BILL_CORRECTED, CASE_CLOSED, METER_OVERRIDE, COMPENSATION_APPROVED }
enum class AuditStatus { PENDING, SUBMITTED, VERIFIED, FAILED }
enum class AuditVerificationStatus { VERIFIED, MISMATCH, PENDING }

@Entity
@Table(name = "audit_events")
class AuditEventEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "case_id", nullable = false, length = 64) var caseId: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 64) var eventType: AuditEventType = AuditEventType.BILL_CORRECTED,
    @Column(name = "payload_hash", nullable = false, length = 64) var payloadHash: String = "",
    @Column(name = "solana_signature", length = 128) var solanaSignature: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) var status: AuditStatus = AuditStatus.PENDING,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.EPOCH
)
