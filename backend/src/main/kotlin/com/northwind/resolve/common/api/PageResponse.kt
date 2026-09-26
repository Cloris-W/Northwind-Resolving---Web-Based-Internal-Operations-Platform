package com.northwind.resolve.common.api

data class PageMetadata(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class PageResponse<T>(val items: List<T>, val page: PageMetadata)
