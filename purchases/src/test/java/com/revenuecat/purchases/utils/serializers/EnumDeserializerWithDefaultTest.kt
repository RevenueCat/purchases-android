package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class EnumDeserializerWithDefaultTest {

    private enum class Color { RED, GREEN, BLUE }

    private object ColorDeserializer : EnumDeserializerWithDefault<Color>(
        serialName = "com.revenuecat.purchases.test.Color",
        defaultValue = Color.RED,
    )

    // A second enum whose simple name collides with [Color] once obfuscated would previously have
    // produced the same descriptor name. The serial name must come from the explicit parameter so
    // these stay distinct.
    private enum class OtherColor { CYAN, MAGENTA }

    private object OtherColorDeserializer : EnumDeserializerWithDefault<OtherColor>(
        serialName = "com.revenuecat.purchases.test.OtherColor",
        defaultValue = OtherColor.CYAN,
    )

    @Test
    fun `descriptor uses the explicit serial name, not the runtime class name`() {
        // In release builds the runtime class name is obfuscated (e.g. "f0"), so deriving the
        // descriptor name from it caused kotlinx serial-name collisions at runtime. The name must
        // be the explicit, stable value passed in.
        assertThat(ColorDeserializer.descriptor.serialName)
            .isEqualTo("com.revenuecat.purchases.test.Color")
        assertThat(ColorDeserializer.descriptor.serialName)
            .isNotEqualTo(Color::class.java.simpleName)
    }

    @Test
    fun `distinct deserializers expose distinct stable serial names`() {
        assertThat(ColorDeserializer.descriptor.serialName)
            .isNotEqualTo(OtherColorDeserializer.descriptor.serialName)
    }

    @Test
    fun `deserializes a known value`() {
        assertThat(Json.decodeFromString(ColorDeserializer, "\"green\"")).isEqualTo(Color.GREEN)
    }

    @Test
    fun `falls back to the default on an unknown value`() {
        assertThat(Json.decodeFromString(ColorDeserializer, "\"purple\"")).isEqualTo(Color.RED)
    }
}
