package com.northwind.resolve.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("northwind.gemini")
data class GeminiProperties(
    var apiKey: String = "",
    var model: String = "gemini-3.8-flash",
    var timeout: Duration = Duration.ofSeconds(10),
    var maxOutputTokens: Int = 1024,
)
