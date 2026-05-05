package com.revenuecat.purchases.common.networking

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
internal data class RemoteConfigResponse(
    @SerialName("config_version") val configVersion: String,
    @SerialName("api_sources") val apiSources: List<ApiSource> = emptyList(),
    @SerialName("asset_sources") val assetSources: List<AssetSource> = emptyList(),
    val manifest: Manifest = Manifest(),
)

@Serializable
internal data class ApiSource(
    val id: String,
    @SerialName("url_prefix") val urlPrefix: String,
    val priority: Int,
    val weight: Int,
    @SerialName("blacklist_time_seconds") val blacklistTimeSeconds: Long,
)

@Serializable
internal data class AssetSource(
    val id: String,
    @SerialName("url_format") val urlFormat: String,
    val priority: Int,
    val weight: Int,
    @SerialName("blacklist_time_seconds") val blacklistTimeSeconds: Long,
    @SerialName("test_url") val testUrl: String? = null,
)

@Serializable
internal data class Manifest(
    @Serializable(with = TopicsMapSerializer::class)
    val topics: Map<Topic, Map<String, TopicEntry>> = emptyMap(),
)

internal enum class Topic(val key: String) {
    PRODUCT_ENTITLEMENT_MAPPING("product_entitlement_mapping"),
    ;

    companion object {
        fun fromKey(key: String): Topic? = values().firstOrNull { it.key == key }
    }
}

@Serializable
internal data class TopicEntry(
    @SerialName("asset_blob_ref") val assetBlobRef: String,
    @SerialName("content_type") val contentType: String,
    val prefetch: Boolean = false,
)

internal object TopicsMapSerializer : KSerializer<Map<Topic, Map<String, TopicEntry>>> {
    private val delegate = MapSerializer(
        String.serializer(),
        MapSerializer(String.serializer(), TopicEntry.serializer()),
    )

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<Topic, Map<String, TopicEntry>> {
        val raw = delegate.deserialize(decoder)
        return raw.mapNotNull { (key, value) ->
            Topic.fromKey(key)?.let { it to value }
        }.toMap()
    }

    override fun serialize(encoder: Encoder, value: Map<Topic, Map<String, TopicEntry>>) {
        delegate.serialize(encoder, value.mapKeys { it.key.key })
    }
}
