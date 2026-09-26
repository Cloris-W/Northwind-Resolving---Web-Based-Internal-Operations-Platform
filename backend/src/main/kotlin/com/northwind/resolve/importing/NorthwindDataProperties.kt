package com.northwind.resolve.importing

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("northwind.import")
data class NorthwindDataProperties(
    val dataDirectory: String = "../data",
)
