package com.northwind.resolve

import com.northwind.resolve.ai.config.GeminiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties::class)
class NorthwindResolveApplication

fun main(args: Array<String>) {
    runApplication<NorthwindResolveApplication>(*args)
}
