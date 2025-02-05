package com.revenuecat.purchases.common.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * A request object for handling events.
 *
 * @property events A list of [BackendEvent] objects representing the events in the request.
 */
@Serializable
data class EventsRequest internal constructor(
    internal val events: List<BackendEvent>,
) {

    /**
     * Companion object to provide serialization configuration for [EventsRequest].
     */
    companion object {
        /**
         * JSON configuration with custom serialization rules for handling polymorphic [BackendEvent] types.
         *
         * - Uses a `discriminator` field to distinguish subclasses.
         * - Enables encoding of default values.
         * - Ignores unknown keys to ensure forward compatibility.
         */
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

    /**
     * Generates a cache key based on the hash codes of the contained events.
     *
     * @return A list of string representations of hash codes for the events in the request.
     */
    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
