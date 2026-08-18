package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.events.BackendEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal sealed class JsonProvider {
    companion object {
        val defaultJson = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendEvent::class) {
                    subclass(BackendEvent.CustomerCenter::class, BackendEvent.CustomerCenter.serializer())
                    subclass(BackendEvent.Paywalls::class, BackendEvent.Paywalls.serializer())
                    subclass(BackendEvent.Workflows::class, BackendEvent.Workflows.serializer())
                }
            }
            classDiscriminator = "discriminator"
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
    }
}
