package com.northwind.resolve.importing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.nio.file.Files
import java.nio.file.Path

class NorthwindCsvParser(private val csvMapper: CsvMapper = CsvMapper()) {
    fun parse(path: Path, requiredHeaders: Set<String>): List<Map<String, String>> {
        require(Files.isRegularFile(path)) { "Missing source CSV: $path" }
        val iterator = csvMapper.readerFor(Map::class.java)
            .with(CsvSchema.emptySchema().withHeader())
            .readValues<Map<*, *>>(path.toFile())

        val rows = iterator.use {
            generateSequence { if (it.hasNext()) it.next() else null }
                .map { row ->
                    (row as Map<*, *>).entries.associate { entry ->
                        entry.key.toString() to (entry.value?.toString()?.trim() ?: "")
                    }
                }
                .toList()
        }
        require(rows.isNotEmpty()) { "Source CSV has no rows: $path" }
        val headers = rows.first().keys
        require(headers == requiredHeaders) {
            "Unexpected headers in $path. Expected $requiredHeaders but found $headers"
        }
        return rows
    }
}
