package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PolymorphicSerializerWithDefaultTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- test fixture ---

    @Serializable(with = EventSerializer::class)
    private sealed class Event {
        @Serializable
        data class Click(@SerialName("target_id") val targetId: String) : Event()

        @Serializable
        data class Scroll(val direction: String, val amount: String) : Event()

        @Serializable
        object Unknown : Event()
    }

    private object EventSerializer : PolymorphicSerializerWithDefault<Event>(
        baseClass = Event::class,
        unknownSerializer = Event.Unknown.serializer(),
    ) {
        override fun selectByType(type: String, element: JsonObject): DeserializationStrategy<Event>? =
            when (type) {
                "click" -> if (element["target_id"] != null) Event.Click.serializer() else null
                "scroll" -> if (element["direction"] != null && element["amount"] != null) {
                    Event.Scroll.serializer()
                } else {
                    null
                }
                else -> null
            }
    }

    @Serializable(with = TaggedEventSerializer::class)
    private sealed class TaggedEvent {
        @Serializable
        data class Tap(val id: String) : TaggedEvent()

        @Serializable
        object Unknown : TaggedEvent()
    }

    private object TaggedEventSerializer : PolymorphicSerializerWithDefault<TaggedEvent>(
        baseClass = TaggedEvent::class,
        unknownSerializer = TaggedEvent.Unknown.serializer(),
        typeField = "kind",
    ) {
        override fun selectByType(type: String, element: JsonObject): DeserializationStrategy<TaggedEvent>? =
            when (type) {
                "tap" -> if (element["id"] != null) TaggedEvent.Tap.serializer() else null
                else -> null
            }
    }

    // --- tests ---

    @Test
    fun `deserializes known type with all fields`() {
        val result = json.decodeFromString<Event>("""{"type":"click","target_id":"btn-1"}""")
        assertThat(result).isEqualTo(Event.Click(targetId = "btn-1"))
    }

    @Test
    fun `deserializes known type with multiple fields`() {
        val result = json.decodeFromString<Event>("""{"type":"scroll","direction":"down","amount":"100"}""")
        assertThat(result).isEqualTo(Event.Scroll(direction = "down", amount = "100"))
    }

    @Test
    fun `falls back to default when selectByType returns null for a known type`() {
        val result = json.decodeFromString<Event>("""{"type":"click"}""")
        assertThat(result).isEqualTo(Event.Unknown)
    }

    @Test
    fun `falls back to default when type is unknown`() {
        val result = json.decodeFromString<Event>("""{"type":"hover","target_id":"btn-1"}""")
        assertThat(result).isEqualTo(Event.Unknown)
    }

    @Test
    fun `falls back to default when type field is absent`() {
        val result = json.decodeFromString<Event>("""{"target_id":"btn-1"}""")
        assertThat(result).isEqualTo(Event.Unknown)
    }

    @Test
    fun `falls back to default for empty object`() {
        val result = json.decodeFromString<Event>("""{}""")
        assertThat(result).isEqualTo(Event.Unknown)
    }

    @Test
    fun `uses custom typeField when specified`() {
        val result = json.decodeFromString<TaggedEvent>("""{"kind":"tap","id":"x"}""")
        assertThat(result).isEqualTo(TaggedEvent.Tap(id = "x"))
    }

    @Test
    fun `falls back to default when custom typeField is absent`() {
        val result = json.decodeFromString<TaggedEvent>("""{"type":"tap","id":"x"}""")
        assertThat(result).isEqualTo(TaggedEvent.Unknown)
    }

    @Test
    fun `ignores unknown extra fields on a recognized variant`() {
        val result = json.decodeFromString<Event>(
            """{"type":"click","target_id":"btn-1","extra":"ignored"}""",
        )
        assertThat(result).isEqualTo(Event.Click(targetId = "btn-1"))
    }
}
