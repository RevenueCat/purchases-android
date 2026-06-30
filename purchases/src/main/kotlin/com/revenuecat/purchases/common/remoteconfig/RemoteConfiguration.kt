package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.JsonProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The `/v1/config` configuration response. This is the JSON payload carried in element 0 (the config
 * element) of the binary [com.revenuecat.purchases.common.networking.RCContainer], and is also the plain
 * JSON fallback body when the SDK does not request the binary format.
 *
 * The [topics] map carries **only changed topic bodies** — a topic whose etag still matches is omitted, and
 * the client keeps its locally cached topic data. [activeTopics] lists **every** active topic name (including
 * unchanged ones), so it is the source of truth for which topics exist and which were removed.
 *
 * [manifest] is an **opaque** token: the SDK stores it verbatim and replays it on the next request so the
 * server can diff against the exact state it issued. The SDK never parses it (the per-topic etags it encodes
 * are server-private).
 *
 * Parsing uses the shared [JsonProvider.defaultJson], which ignores unknown keys so unknown topics and
 * unknown item metadata keys survive, keeping the SDK forward-compatible with topics it does not yet handle.
 */
@Serializable
internal data class RemoteConfiguration(
    val domain: String,
    /** Other domains the SDK should also sync to assemble the full configuration. */
    val subdomains: List<String> = emptyList(),
    @SerialName("app_uuid") val appUuid: String? = null,
    /** Opaque server token; stored verbatim and replayed as the request `manifest`. Never parsed. */
    val manifest: String,
    /** The full set of active topic names, including unchanged ones. Source of truth for topic existence. */
    @SerialName("active_topics") val activeTopics: List<String> = emptyList(),
    /** Blob refs the server believes the SDK should have prefetched for the resolved configuration. */
    @SerialName("prefetch_blobs") val prefetchBlobs: List<String> = emptyList(),
    /** Changed topic bodies only: TopicName -> (ItemName -> [ConfigItem]). */
    val topics: Map<String, ConfigTopic> = emptyMap(),
    @SerialName("state_hash") val stateHash: String? = null,
) {
    /**
     * A single item within a topic. An item is arbitrary, topic-specific JSON; [blobRef] and [prefetch] are
     * the only reserved conventions the SDK interprets, and every other key is preserved verbatim in
     * [content].
     *
     * An item's payload arrives one of two ways, chosen by the server per response:
     * - **inline**: [blobRef] is `null` and the payload is the topic-specific JSON in [content]
     *   (e.g. a `sources` `api`/`blob` descriptor, or an inline `product_entitlement_mapping` entry).
     * - **by reference**: [blobRef] is set and the payload lives in an external blob to fetch/read by that
     *   ref; [content] then holds only whatever extra inline keys (if any) accompanied the reference.
     *
     * Topic handlers resolve the payload as `blobRef?.let { read+parse blob } ?: content`, so both modes work
     * for any topic. Unknown reserved keys are not a concern: anything that is not [blobRef]/[prefetch] is
     * kept in [content], keeping the SDK forward-compatible.
     */
    @Serializable(with = ConfigItemSerializer::class)
    internal data class ConfigItem(
        /** When present, the item's payload is an external static blob addressed by this ref, not inlined. */
        val blobRef: String? = null,
        /** When `true`, the SDK should proactively cache this item's blob. */
        val prefetch: Boolean = false,
        /** The topic-specific item content (all keys except the reserved [blobRef]/[prefetch]). */
        val content: JsonObject = JsonObject(emptyMap()),
    )

    companion object {
        fun parse(bytes: ByteArray): RemoteConfiguration =
            JsonProvider.defaultJson.decodeFromString(serializer(), bytes.decodeToString())

        /** Duplicates [buffer] so the caller's read-only, zero-copy `RCElement.data` view is left untouched. */
        fun parse(buffer: ByteBuffer): RemoteConfiguration {
            val view = buffer.duplicate()
            val bytes = ByteArray(view.remaining())
            view.get(bytes)
            return parse(bytes)
        }
    }
}

private val configItemMapSerializer =
    MapSerializer(String.serializer(), RemoteConfiguration.ConfigItem.serializer())

/**
 * A single topic's items, with a stable [contentHash] so consumers can tell whether a topic changed
 * without re-reading its contents. Delegates [Map], so it can be accessed like the item map it wraps.
 */
@Serializable(with = ConfigTopicSerializer::class)
internal data class ConfigTopic(
    val items: Map<String, RemoteConfiguration.ConfigItem>,
) : Map<String, RemoteConfiguration.ConfigItem> by items {

    /** Stable content hash of [items], computed lazily on first access. */
    val contentHash: String by lazy { computeHash(items) }

    private companion object {
        private const val HEX_BYTE_MASK = 0xFF

        private fun computeHash(items: Map<String, RemoteConfiguration.ConfigItem>): String {
            val element = JsonProvider.defaultJson.encodeToJsonElement(configItemMapSerializer, items)
            return sha256Hex(canonicalize(element).toString())
        }

        // Sorts object keys recursively so logically-equal topics hash the same regardless of key order.
        // Array order is preserved (it can be meaningful, e.g. the sources list).
        private fun canonicalize(element: JsonElement): JsonElement = when (element) {
            is JsonObject -> JsonObject(
                element.entries.sortedBy { it.key }.associate { it.key to canonicalize(it.value) },
            )
            is JsonArray -> JsonArray(element.map { canonicalize(it) })
            else -> element
        }

        private fun sha256Hex(input: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and HEX_BYTE_MASK) }
    }
}

/**
 * Serializes [ConfigTopic] transparently as its bare item map (no wrapper object), so the wire and persisted
 * shape is identical to the previous `Map` typealias and [ConfigTopic.contentHash] stays a derived, SDK-only value.
 */
internal object ConfigTopicSerializer : KSerializer<ConfigTopic> {
    override val descriptor: SerialDescriptor = configItemMapSerializer.descriptor
    override fun deserialize(decoder: Decoder): ConfigTopic =
        ConfigTopic(configItemMapSerializer.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: ConfigTopic) =
        configItemMapSerializer.serialize(encoder, value.items)
}

/**
 * Serializes [RemoteConfiguration.ConfigItem] as a flat JSON object: the reserved keys `blob_ref`/`prefetch`
 * plus every key of [RemoteConfiguration.ConfigItem.content], so arbitrary inline item content survives a
 * parse -> persist -> parse round-trip.
 */
internal object ConfigItemSerializer : KSerializer<RemoteConfiguration.ConfigItem> {
    private const val BLOB_REF_KEY = "blob_ref"
    private const val PREFETCH_KEY = "prefetch"

    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): RemoteConfiguration.ConfigItem {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ConfigItem can only be deserialized from JSON.")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val blobRef = (obj[BLOB_REF_KEY] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val prefetch = (obj[PREFETCH_KEY] as? JsonPrimitive)?.booleanOrNull ?: false
        val content = JsonObject(obj.filterKeys { it != BLOB_REF_KEY && it != PREFETCH_KEY })
        return RemoteConfiguration.ConfigItem(blobRef = blobRef, prefetch = prefetch, content = content)
    }

    override fun serialize(encoder: Encoder, value: RemoteConfiguration.ConfigItem) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ConfigItem can only be serialized to JSON.")
        val merged = buildMap<String, JsonElement> {
            putAll(value.content)
            value.blobRef?.let { put(BLOB_REF_KEY, JsonPrimitive(it)) }
            if (value.prefetch) put(PREFETCH_KEY, JsonPrimitive(true))
        }
        jsonEncoder.encodeJsonElement(JsonObject(merged))
    }
}
