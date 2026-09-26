package com.northwind.resolve.ai.domain

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

enum class AiAnalysisType { SUMMARY, RECOMMENDATION }

@Entity
@Table(name = "ai_analysis")
class AiAnalysisEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "case_id", nullable = false, length = 64) var caseId: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "analysis_type", nullable = false, length = 32) var analysisType: AiAnalysisType = AiAnalysisType.SUMMARY,
    @Column(name = "input_hash", nullable = false, length = 64) var inputHash: String = "",
    @Column(nullable = false, length = 128) var model: String = "",
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "output_json", nullable = false, columnDefinition = "jsonb") var outputJson: String = "{}",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.EPOCH
)
