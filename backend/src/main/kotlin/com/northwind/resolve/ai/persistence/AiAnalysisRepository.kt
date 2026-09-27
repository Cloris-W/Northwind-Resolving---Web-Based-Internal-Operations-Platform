package com.northwind.resolve.ai.persistence

import com.northwind.resolve.ai.domain.AiAnalysisEntity
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.ZoneOffset

@Repository
@Profile("database")
class AiAnalysisRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun insert(analysis: AiAnalysisEntity) {
        jdbc.update("""INSERT INTO ai_analysis (id, case_id, analysis_type, input_hash, model, output_json, created_at)
            VALUES (:id, :caseId, :analysisType, :inputHash, :model, CAST(:outputJson AS jsonb), :createdAt)""",
            mapOf("id" to analysis.id, "caseId" to analysis.caseId, "analysisType" to analysis.analysisType.name,
                "inputHash" to analysis.inputHash, "model" to analysis.model, "outputJson" to analysis.outputJson,
                "createdAt" to analysis.createdAt.atOffset(ZoneOffset.UTC)))
    }
}
