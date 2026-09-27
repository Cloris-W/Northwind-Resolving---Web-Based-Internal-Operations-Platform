package com.northwind.resolve.ai.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.northwind.resolve.ai.application.AiUnavailableException
import com.northwind.resolve.ai.application.GeminiBillingFact
import com.northwind.resolve.ai.application.GeminiCaseContextInput
import com.northwind.resolve.ai.application.GeminiCaseFact
import com.northwind.resolve.ai.application.GeminiMeterFact
import com.northwind.resolve.ai.config.GeminiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestClient
import java.time.Instant

class GeminiRestAdapterTests {
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val input = GeminiCaseContextInput(GeminiCaseFact("BILLING", "HIGH", "Test", "OPEN", 10, Instant.parse("2026-01-01T00:00:00Z")), emptyList(), GeminiBillingFact(null, 0), GeminiMeterFact(false, null), emptyList(), false, false)

    @Test
    fun `missing API key fails only when AI is invoked`() {
        assertThrows(AiUnavailableException::class.java) { GeminiRestAdapter(GeminiProperties(apiKey = ""), mapper).summary(input) }
    }

    @Test fun `summary posts exact approved interactions request and extracts output`() {
        val builder = RestClient.builder(); val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions")).andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key", "test-key")).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().string(org.hamcrest.Matchers.containsString("\"store\":false")))
            .andRespond(withSuccess(completed("""{"summary":"summary","classification":"classification","customerResponseDraft":"draft"}"""), MediaType.APPLICATION_JSON))
        val result = GeminiRestAdapter(GeminiProperties(apiKey = "test-key", model = "test-model"), mapper, builder.build()).summary(input)
        assertEquals("summary", result.summary); assertEquals("draft", result.customerResponseDraft); server.verify()
    }

    @Test fun `recommendation extracts structured output`() {
        val builder = RestClient.builder(); val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions")).andRespond(withSuccess(completed("""{"recommendation":"review meter","rationale":"evidence"}"""), MediaType.APPLICATION_JSON))
        val result = GeminiRestAdapter(GeminiProperties(apiKey = "test-key"), mapper, builder.build()).recommendation(input)
        assertEquals("review meter", result.recommendation); server.verify()
    }

    @Test fun `malformed incomplete and ambiguous output is unavailable`() {
        listOf("""{"status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":"not-json"}]}]}""", """{"status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":"{\\\"summary\\\":\\\"a\\\",\\\"classification\\\":\\\"b\\\"}"}]}]}""", """{"status":"incomplete","steps":[]}""").forEach { response ->
            val builder = RestClient.builder(); val server = MockRestServiceServer.bindTo(builder).build()
            server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions")).andRespond(withSuccess(response, MediaType.APPLICATION_JSON))
            assertThrows(AiUnavailableException::class.java) { GeminiRestAdapter(GeminiProperties(apiKey = "test-key"), mapper, builder.build()).summary(input) }; server.verify()
        }
    }

    @Test fun `quota server and timeout responses are unavailable`() {
        listOf(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, org.springframework.http.HttpStatus.BAD_GATEWAY, org.springframework.http.HttpStatus.REQUEST_TIMEOUT).forEach { status ->
            val builder = RestClient.builder(); val server = MockRestServiceServer.bindTo(builder).build()
            server.expect(requestTo("https://generativelanguage.googleapis.com/v1/interactions")).andRespond(withStatus(status))
            assertThrows(AiUnavailableException::class.java) { GeminiRestAdapter(GeminiProperties(apiKey = "test-key"), mapper, builder.build()).summary(input) }; server.verify()
        }
    }

    private fun completed(output: String) = mapper.writeValueAsString(mapOf("status" to "completed", "steps" to listOf(mapOf("type" to "model_output", "content" to listOf(mapOf("type" to "text", "text" to output))))))
}
