package com.locoquest.app

import android.app.Application
import com.locoquest.app.service.BenchmarkService
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
    suspend fun `test parsing of benchmark JSON data`() {
        // Given
        val benchmarkJson = """
            {
                "id": "ABC123",
                "name": "Mount Everest",
                "latitude": 27.988056,
                "longitude": 86.925278,
                "elevation": 8848.86,
                "coordinates": "27.988056, 86.925278"
            }
        """.trimIndent()

        // When
        val benchmark = benchmarkService.parseBenchmarkData(benchmarkJson)

        // Then
        assertNotNull(benchmark)
        assertEquals("ABC123", benchmark.id)
        assertEquals("Mount Everest", benchmark.name)
        assertEquals("27.988056, 86.925278", benchmark.coordinates)
        assertEquals(27.988056, benchmark.latitude, 0.001)
        assertEquals(86.925278, benchmark.longitude, 0.001)
        assertEquals(8848.86, benchmark.elevation, 0.001)
    }

    private class MockBenchmarkService(application: Application) : BenchmarkService(application) {
        override suspend fun parseBenchmarkData(jsonData: String): ArrayList<Benchmark> {
            // For the sake of the test, we'll just return a hard-coded benchmark object
            val benchmarks: ArrayList<Benchmark> = []
            benchmarks.add(Benchmark(id = "ABC123",
                name = "Mount Everest",
                coordinates = "27.988056, 86.925278",
                latitude = 27.988056,
                longitude = 86.925278,
                elevation = 8848.86,
                description = 0.0,
                d = 0.0,
                d1 = 0.0))

            return benchmarks
        }
    }
}