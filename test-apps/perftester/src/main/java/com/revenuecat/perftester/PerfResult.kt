package com.revenuecat.perftester

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PerfResult(
    @SerialName("schema_version") val schemaVersion: Int = 1,
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
