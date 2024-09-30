package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen.ScreenType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

internal object ScreenMapSerializer : KSerializer<Map<ScreenType, Screen>> {
    override val descriptor: SerialDescriptor = MapSerializer(ScreenType.serializer(), Screen.serializer()).descriptor

    override fun deserialize(decoder: Decoder): Map<ScreenType, Screen> {
        val map = mutableMapOf<ScreenType, Screen>()
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val jsonObject = jsonInput.decodeJsonElement().jsonObject

        jsonObject.forEach { (key, value) ->
            try {
                val enumKey = ScreenType.valueOf(key)
                map[enumKey] = jsonInput.json.decodeFromJsonElement(Screen.serializer(), value)
            } catch (_: IllegalArgumentException) {
                debugLog("Unknown CustomerCenter ScreenType: $key. Ignoring.")
            }
        }

        return map
    }

    override fun serialize(encoder: Encoder, value: Map<ScreenType, Screen>) {
        MapSerializer(ScreenType.serializer(), Screen.serializer()).serialize(encoder, value)
    }
}
