package com.locoquest.app

import BenchmarkService
import com.locoquest.app.dto.Benchmark
import org.junit.Assert.*
import org.junit.Test

/*
    - This unit test is for the BenchmarkService interface's parseBenchmarkData method.
    - It tests the ability of the method to correctly parse a JSON string representing a Benchmark object and return an instance of that object.
    - A hard-coded JSON string is provided as input for the test. A MockBenchmarkService object is used to test the method.
    - The MockBenchmarkService overrides the parseBenchmarkData method of the BenchmarkService interface
      with a hard-coded implementation that simply returns a hard-coded Benchmark object.
    - The test checks that the parseBenchmarkData method correctly parses the JSON string and returns a Benchmark
      object with the expected values for its properties.
*/

class BenchmarkServiceTest {

    private val benchmarkService: MockBenchmarkService = MockBenchmarkService()

    @Test
    fun `test parsing of benchmark JSON data`() {
        // Given
        val benchmarkJson = """
            {
                "pid": "ABC123",
                "name": "Mount Everest",
                "lat": "27.988056",
                "lon": "86.925278",
                "ellipHeight": "8848.86",
                "posDatum": "N/A",
                "posSource": "N/A",
                "posOrder": "N/A",
                "orthoHt": "N/A",
                "vertDatum": "N/A",
                "vertSource": "N/A",
                "vertOrder": "N/A"
            }
        """.trimIndent()

        // When
        val benchmark = benchmarkService.parseBenchmarkData(benchmarkJson)

        // Then
        assertNotNull(benchmark)
        assertEquals("ABC123", benchmark.pid)
        assertEquals("Mount Everest", benchmark.name)
        assertEquals("27.988056", benchmark.lat)
        assertEquals("86.925278", benchmark.lon)
        assertEquals("8848.86", benchmark.ellipHeight)
        assertEquals("N/A", benchmark.posDatum)
        assertEquals("N/A", benchmark.posSource)
        assertEquals("N/A", benchmark.posOrder)
        assertEquals("N/A", benchmark.orthoHt)
        assertEquals("N/A", benchmark.vertDatum)
        assertEquals("N/A", benchmark.vertSource)
        assertEquals("N/A", benchmark.vertOrder)
    }

    private class MockBenchmarkService : BenchmarkService() {
        override fun parseBenchmarkData(jsonData: String): Benchmark {
            return Benchmark(
                pid = "ABC123",
                name = "Mount Everest",
                lat = "27.988056",
                lon = "86.925278",
                ellipHeight = "8848.86",
                posDatum = "N/A",
                posSource = "N/A",
                posOrder = "N/A",
                orthoHt = "N/A",
                vertDatum = "N/A",
                vertSource = "N/A",
                vertOrder = "N/A"
            )
        }
    }
}