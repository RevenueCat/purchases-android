package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.events.BackendEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal object JsonHelper {
    companion object {
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendEvent::class) {
                    subclass(BackendEvent.CustomerCenter::class, BackendEvent.CustomerCenter.serializer())
                    subclass(BackendEvent.Paywalls::class, BackendEvent.Paywalls.serializer())
                }
            }
            classDiscriminator = "discriminator"
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
    }
}
