package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.debugLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal object HelpPathsSerializer : KSerializer<List<CustomerCenterConfigData.HelpPath>> {
    override val descriptor: SerialDescriptor = ListSerializer(
        CustomerCenterConfigData.HelpPath.serializer(),
    ).descriptor

    override fun deserialize(decoder: Decoder): List<CustomerCenterConfigData.HelpPath> {
        val list = mutableListOf<CustomerCenterConfigData.HelpPath>()
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val jsonArray = jsonInput.decodeJsonElement().jsonArray

        jsonArray.forEach { jsonElement ->
            try {
                list.add(
                    jsonInput.json.decodeFromJsonElement(CustomerCenterConfigData.HelpPath.serializer(), jsonElement),
                )
            } catch (e: IllegalArgumentException) {
                debugLog("Issue deserializing CustomerCenter HelpPath. Ignoring. Error: $e")
            }
        }

        return list
    }

    override fun serialize(encoder: Encoder, value: List<CustomerCenterConfigData.HelpPath>) {
        ListSerializer(CustomerCenterConfigData.HelpPath.serializer()).serialize(encoder, value)
    }
}
