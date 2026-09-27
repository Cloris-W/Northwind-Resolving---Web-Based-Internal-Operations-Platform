package com.northwind.resolve

import com.northwind.resolve.ai.config.GeminiProperties
import com.northwind.resolve.audit.config.SolanaProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GeminiProperties::class, SolanaProperties::class)
class NorthwindResolveApplication

fun main(args: Array<String>) {
    runApplication<NorthwindResolveApplication>(*args)
}
