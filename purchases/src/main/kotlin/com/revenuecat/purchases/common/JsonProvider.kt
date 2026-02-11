package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.events.BackendEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal sealed class JsonProvider {
    public companion object {
        val defaultJson = Json {
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
