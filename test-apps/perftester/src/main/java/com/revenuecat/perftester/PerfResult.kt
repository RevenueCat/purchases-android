package com.revenuecat.perftester

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PerfResult(
    @SerialName("schema_version") val schemaVersion: Int = 2,
    @SerialName("source") val source: String = "perftester-app",
    @SerialName("timestamp_epoch_ms") val timestampEpochMs: Long,
    @SerialName("sdk_version") val sdkVersion: String,
    @SerialName("device_model") val deviceModel: String,
    @SerialName("android_api_level") val androidApiLevel: Int,
    @SerialName("workflows_enabled") val workflowsEnabled: Boolean,
    @SerialName("press_index_in_process") val pressIndexInProcess: Int,
    @SerialName("process_start_to_press_ms") val processStartToPressMs: Long,
    @SerialName("had_offerings_cache") val hadOfferingsCache: Boolean,
    @SerialName("had_remote_config_cache") val hadRemoteConfigCache: Boolean,
    @SerialName("had_blobs_cache") val hadBlobsCache: Boolean,
    @SerialName("memory_before_rss_kb") val memoryBeforeRssKb: Long?,
    @SerialName("memory_after_rss_kb") val memoryAfterRssKb: Long?,
    @SerialName("memory_delta_rss_kb") val memoryDeltaRssKb: Long?,
    @SerialName("memory_before_rss_anon_kb") val memoryBeforeRssAnonKb: Long?,
    @SerialName("memory_after_rss_anon_kb") val memoryAfterRssAnonKb: Long?,
    @SerialName("memory_delta_rss_anon_kb") val memoryDeltaRssAnonKb: Long?,
    @SerialName("memory_before_rss_file_kb") val memoryBeforeRssFileKb: Long?,
    @SerialName("memory_after_rss_file_kb") val memoryAfterRssFileKb: Long?,
    @SerialName("memory_delta_rss_file_kb") val memoryDeltaRssFileKb: Long?,
    @SerialName("memory_before_java_heap_kb") val memoryBeforeJavaHeapKb: Long?,
    @SerialName("memory_after_java_heap_kb") val memoryAfterJavaHeapKb: Long?,
    @SerialName("memory_delta_java_heap_kb") val memoryDeltaJavaHeapKb: Long?,
    @SerialName("memory_before_native_heap_kb") val memoryBeforeNativeHeapKb: Long?,
    @SerialName("memory_after_native_heap_kb") val memoryAfterNativeHeapKb: Long?,
    @SerialName("memory_delta_native_heap_kb") val memoryDeltaNativeHeapKb: Long?,
    @SerialName("configure_to_offerings_ms") val configureToOfferingsMs: Double?,
    @SerialName("get_offerings_ms") val getOfferingsMs: Double?,
    @SerialName("ui_config_ms") val uiConfigMs: Double?,
    @SerialName("ui_config_is_default") val uiConfigIsDefault: Boolean?,
    @SerialName("workflow_resolution_ms") val workflowResolutionMs: Double?,
    @SerialName("workflow_body_ms") val workflowBodyMs: Double?,
    @SerialName("workflow_resolved") val workflowResolved: Boolean?,
    @SerialName("success") val success: Boolean,
    @SerialName("error_message") val errorMessage: String?,
    @SerialName("offerings_count") val offeringsCount: Int?,
    @SerialName("current_offering_id") val currentOfferingId: String?,
) {
    fun toJsonLine(): String = json.encodeToString(serializer(), this)

    private companion object {
        private val json = Json { encodeDefaults = true }
    }
}
