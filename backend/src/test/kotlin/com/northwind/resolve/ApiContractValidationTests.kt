package com.northwind.resolve

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ApiContractValidationTests {
    @Test
    fun `canonical OpenAPI contract parses and defines the MVP operations`() {
        val specification = Path.of("..", "docs", "api-contract.yaml").toAbsolutePath().toString()
        val options = ParseOptions().apply {
            isResolve = true
            isResolveFully = true
        }

        val result = OpenAPIV3Parser().readLocation(specification, null, options)
        assertTrue(result.messages.orEmpty().isEmpty(), "OpenAPI parse errors: ${result.messages}")

        val openApi = result.openAPI
        assertNotNull(openApi)
        assertEquals("3.0.3", openApi.openapi)

        val expectedPaths = setOf(
            "/api/cases",
            "/api/cases/{caseId}",
            "/api/cases/{caseId}/timeline",
            "/api/cases/{caseId}/transfer",
            "/api/cases/{caseId}/field-visit",
            "/api/accounts/{accountId}/meter-readings",
            "/api/accounts/{accountId}/billing",
            "/api/billing/exceptions",
            "/api/billing/exceptions/{id}",
            "/api/billing/exceptions/{id}/review",
            "/api/ai/cases/{caseId}/summary",
            "/api/ai/cases/{caseId}/recommendation",
            "/api/audit/events",
            "/api/audit/events/{id}/verify",
            "/api/dashboard/kpis",
            "/api/value-case"
        )

        assertEquals(expectedPaths, openApi.paths.keys)
        assertTrue(
            openApi.components.schemas.keys.containsAll(
                setOf("ApiError", "ValidationError", "Case", "BillingException", "BillCorrection", "AuditEvent")
            )
        )

        val operationIds = openApi.paths.values.flatMap { it.readOperations() }.map { it.operationId }
        assertEquals(16, operationIds.size)
        assertEquals(operationIds.size, operationIds.toSet().size)
        assertTrue(operationIds.all { it.matches(Regex("[a-z][A-Za-z0-9]*")) })
    }
}
