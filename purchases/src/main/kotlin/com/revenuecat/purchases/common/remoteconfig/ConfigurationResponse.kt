package com.revenuecat.purchases.common.remoteconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The `/v2/config` configuration response. This is the JSON payload carried in element 0 (the config
 * element) of the binary [com.revenuecat.purchases.common.networking.RCContainer], and is also the plain
 * JSON fallback body when the SDK does not request the binary format.
 *
 * The [topics] map carries **only changed topic bodies** — a topic whose client-sent etag still matches is
 * omitted, and the client keeps its locally cached topic data. [manifest] lists **every** active topic and
 * its current etag (including unchanged ones), so it is the source of truth for which topics exist.
 *
 * Parsing is lenient ([Json.ignoreUnknownKeys]) so unknown topics and unknown item metadata keys survive,
 * keeping the SDK forward-compatible with topics it does not yet handle.
 */
@Serializable
internal data class ConfigurationResponse(
    /** Configuration domain this response belongs to (default `app`). */
    val domain: String,
    /** Other domains the SDK should also sync to assemble the full configuration. Omitted when none. */
    val subdomains: List<String> = emptyList(),
    @SerialName("app_uuid") val appUuid: String? = null,
    /** The fresh server manifest to persist and replay on the next request. */
    val manifest: ConfigManifest,
    /** Changed topic bodies only: TopicName -> (ItemName -> [ConfigItem]). */
    val topics: Map<String, ConfigTopic> = emptyMap(),
    @SerialName("state_hash") val stateHash: String? = null,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Parses the config element bytes (UTF-8 JSON) into a [ConfigurationResponse]. */
        fun parse(bytes: ByteArray): ConfigurationResponse =
            json.decodeFromString(serializer(), bytes.decodeToString())
    }
}

/**
 * A configuration manifest. The SDK persists the server-sent manifest and replays it as the request
 * `manifest` on each subsequent call so the server can return only what changed.
 */
@Serializable
internal data class ConfigManifest(
    /** Configuration domain the manifest belongs to. Defaults to `app`. */
    val domain: String,
    /** Topic name -> compact topic etag. Lists every active topic, including unchanged ones. */
    val topics: Map<String, String> = emptyMap(),
    /** Blob refs the server believes the SDK should have prefetched for the resolved configuration. */
    @SerialName("prefetch_blobs") val prefetchBlobs: List<String> = emptyList(),
    /** Blob refs the SDK has actually cached locally. Sent on the request; absent from server responses. */
    @SerialName("prefetched_blobs") val prefetchedBlobs: List<String> = emptyList(),
    /** Timestamp from the previous server manifest. Used only for refresh cadence. */
    @SerialName("last_refresh_at") val lastRefreshAt: Long = 0,
)

/** A topic body: a map of ItemName to its [ConfigItem] metadata. */
internal typealias ConfigTopic = Map<String, ConfigItem>

/**
 * Item metadata within a topic. The metadata is arbitrary JSON; [blobRef] and [prefetch] are the reserved
 * conventions the SDK relies on. Unknown keys are ignored during parsing.
 */
@Serializable
internal data class ConfigItem(
    /** When present, the item has an external static blob payload to fetch, addressed by this ref. */
    @SerialName("blob_ref") val blobRef: String? = null,
    /** When true, the SDK should proactively cache the [blobRef] blob. */
    val prefetch: Boolean = false,
)
