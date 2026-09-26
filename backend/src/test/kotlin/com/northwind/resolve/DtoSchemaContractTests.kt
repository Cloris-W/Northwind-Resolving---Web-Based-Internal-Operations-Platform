package com.northwind.resolve

import com.northwind.resolve.ai.api.AiCaseRecommendationDto
import com.northwind.resolve.ai.api.AiCaseSummaryDto
import com.northwind.resolve.audit.api.AuditEventDto
import com.northwind.resolve.audit.api.AuditVerificationResponse
import com.northwind.resolve.audit.api.CreateAuditEventRequest
import com.northwind.resolve.audit.domain.AuditEventType
import com.northwind.resolve.audit.domain.AuditStatus
import com.northwind.resolve.audit.domain.AuditVerificationStatus
import com.northwind.resolve.billing.api.BillCorrectionDto
import com.northwind.resolve.billing.api.BillingExceptionDto
import com.northwind.resolve.billing.api.BillingRecordStatus
import com.northwind.resolve.billing.domain.BillingExceptionStatus
import com.northwind.resolve.billing.domain.BillingReviewAction
import com.northwind.resolve.billing.domain.RiskLevel
import com.northwind.resolve.cases.api.CaseDto
import com.northwind.resolve.cases.api.CaseEventDto
import com.northwind.resolve.cases.domain.CaseCategory
import com.northwind.resolve.cases.domain.CaseEventType
import com.northwind.resolve.cases.domain.CasePriority
import com.northwind.resolve.cases.domain.CaseStatus
import com.northwind.resolve.cases.domain.SourceSystem
import com.northwind.resolve.fieldforce.api.FieldVisitDto
import com.northwind.resolve.fieldforce.domain.FieldVisitStatus
import com.northwind.resolve.metering.api.MeterReadingDto
import io.swagger.v3.parser.OpenAPIV3Parser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class DtoSchemaContractTests {
    private val contract by lazy {
        OpenAPIV3Parser().readLocation("../docs/api-contract.yaml", null, null).openAPI
    }

    @Test
    fun `DTO property names are contained by their matching OpenAPI schemas`() {
        val dtoMappings = mapOf<KClass<*>, String>(
            CaseDto::class to "Case",
            CaseEventDto::class to "CaseEvent",
            MeterReadingDto::class to "MeterReading",
            BillCorrectionDto::class to "BillCorrection",
            BillingExceptionDto::class to "BillingException",
            FieldVisitDto::class to "FieldVisit",
            AiCaseSummaryDto::class to "AiCaseSummary",
            AiCaseRecommendationDto::class to "AiCaseRecommendation",
            CreateAuditEventRequest::class to "CreateAuditEventRequest",
            AuditEventDto::class to "AuditEvent",
            AuditVerificationResponse::class to "AuditVerificationResponse"
        )

        dtoMappings.forEach { (dtoType, schemaName) ->
            val dtoProperties = dtoType.primaryConstructor!!.parameters.mapNotNull { it.name }.toSet()
            val schemaProperties = contract.components.schemas[schemaName]!!.properties.keys
            assertTrue(schemaProperties.containsAll(dtoProperties), "$schemaName does not contain all ${dtoType.simpleName} properties")
        }
    }

    @Test
    fun `Kotlin enum values match OpenAPI enum values`() {
        val enumMappings = mapOf(
            "CaseStatus" to CaseStatus.values().map { it.name },
            "CaseCategory" to CaseCategory.values().map { it.name },
            "CasePriority" to CasePriority.values().map { it.name },
            "CaseEventType" to CaseEventType.values().map { it.name },
            "SourceSystem" to SourceSystem.values().map { it.name },
            "FieldVisitStatus" to FieldVisitStatus.values().map { it.name },
            "BillingExceptionStatus" to BillingExceptionStatus.values().map { it.name },
            "RiskLevel" to RiskLevel.values().map { it.name },
            "BillingReviewAction" to BillingReviewAction.values().map { it.name },
            "AuditEventType" to AuditEventType.values().map { it.name },
            "AuditStatus" to AuditStatus.values().map { it.name },
            "AuditVerificationStatus" to AuditVerificationStatus.values().map { it.name }
        )

        enumMappings.forEach { (schemaName, values) ->
            val contractValues = contract.components.schemas[schemaName]!!.enum.map { it.toString() }
            assertEquals(values, contractValues, "$schemaName enum differs from the canonical contract")
        }

        val billingRecordStatuses = contract.components.schemas["BillingRecord"]!!.properties["status"]!!.enum
            .map { it.toString() }
        assertEquals(BillingRecordStatus.values().map { it.name }, billingRecordStatuses)
    }
}
