package com.revenuecat.purchases.common.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class EventRequest internal constructor(
    internal val events: List<Map<String, JsonElement>?>,
) {

    companion object {
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendEvent::class) {
                    subclass(BackendEvent.CustomerCenter::class, BackendEvent.CustomerCenter.serializer())
                    subclass(BackendEvent.Paywalls::class, BackendEvent.Paywalls.serializer())
                }
            }
            classDiscriminator = "discriminator"
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
