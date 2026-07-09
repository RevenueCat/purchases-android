package com.revenuecat.perftester

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PerfResultTest {

    private val result = PerfResult(
        timestampEpochMs = 1720000000000L,
        sdkVersion = "10.14.0-SNAPSHOT",
        deviceModel = "Google sdk_gphone64_arm64",
        androidApiLevel = 34,
        workflowsEnabled = true,
        pressIndexInProcess = 1,
        processStartToPressMs = 1234,
        hadOfferingsCache = false,
        hadRemoteConfigCache = false,
        hadBlobsCache = false,
        configureToOfferingsMs = 321.5,
        getOfferingsMs = 300.25,
        uiConfigMs = 42.0,
        uiConfigIsDefault = false,
        workflowResolutionMs = 1.5,
        workflowBodyMs = 55.0,
        workflowResolved = true,
        success = true,
        errorMessage = null,
        offeringsCount = 3,
        currentOfferingId = "default",
    )

    @Test
    fun `json line contains exactly the schema field names`() {
        val parsed = Json.parseToJsonElement(result.toJsonLine()).jsonObject
        val expectedKeys = setOf(
            "schema_version", "source", "timestamp_epoch_ms", "sdk_version", "device_model",
            "android_api_level", "workflows_enabled", "press_index_in_process",
            "process_start_to_press_ms", "had_offerings_cache", "had_remote_config_cache",
            "had_blobs_cache", "configure_to_offerings_ms", "get_offerings_ms", "ui_config_ms",
            "ui_config_is_default", "workflow_resolution_ms", "workflow_body_ms",
            "workflow_resolved", "success", "error_message", "offerings_count",
            "current_offering_id",
        )
        assertEquals(expectedKeys, parsed.keys)
        assertEquals("1", parsed["schema_version"]!!.jsonPrimitive.content)
        assertEquals("perftester-app", parsed["source"]!!.jsonPrimitive.content)
    }

    @Test
    fun `nulls are written explicitly so every line has every field`() {
        val parsed = Json.parseToJsonElement(result.copy(errorMessage = null).toJsonLine()).jsonObject
        assertTrue(parsed.containsKey("error_message"))
    }

    @Test
    fun `writer appends one line per result`() {
        val file = File.createTempFile("results", ".jsonl")
        val writer = ResultsWriter(file)
        writer.append(result)
        writer.append(result.copy(pressIndexInProcess = 2))
        val lines = file.readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("\"press_index_in_process\":2"))
        file.delete()
    }
}
