package com.northwind.resolve.importing

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("database")
@EnableConfigurationProperties(NorthwindDataProperties::class)
class NorthwindImportConfiguration {
    @Bean
    fun northwindDataImportRunner(importService: NorthwindCsvImportService): ApplicationRunner =
        ApplicationRunner { importService.importAll() }
}
