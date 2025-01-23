package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class FallbackComponentTests {

    @Test
    fun `Should deserialize the fallback if the type is unknown`() {
        // Arrange
        // language=json
        val serialized = """
        {
          "type": "super_new_type",
          "unknown_property": {
            "type": "more_unknown"
          },
          "fallback": {
            "components": [
              {
                "color": {
                  "light": {
                    "type": "alias",
                    "value": "primary"
                  }
                },
                "components": [],
                "id": "xmpgCrN9Rb",
                "name": "Text",
                "text_lid": "7bkohQjzIE",
                "type": "text"
              }
            ],
            "id": "WLbwQoNUKF",
            "name": "Stack",
            "type": "stack"
          }
        }
        """.trimIndent()
        val expected = StackComponent(
            components = listOf(
                TextComponent(
                    text = LocalizationKey("7bkohQjzIE"),
                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                )
            )
        )

        // Act
        val actual = OfferingParser.json.decodeFromString<PaywallComponent>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should fail to deserialize an unknown type without a fallback`() {
        // Arrange
        // language=json
        val serialized = """
        {
          "type": "super_new_type",
          "unknown_property": {
            "type": "more_unknown"
          }
        }
        """.trimIndent()

        // Act
        val actual = try{
            OfferingParser.json.decodeFromString<PaywallComponent>(serialized)
            error("Should have thrown an exception")
        } catch (e: SerializationException) {
            e
        }

        // Assert
        assertThat(actual).isInstanceOf(SerializationException::class.java)
        assertThat(actual.message).isEqualTo("No fallback provided for unknown type: super_new_type")
    }

    @Test
    fun `Should fail to deserialize an unknown type with a null fallback`() {
        // Arrange
        // language=json
        val serialized = """
        {
          "type": "super_new_type",
          "unknown_property": {
            "type": "more_unknown"
          },
          "fallback": null
        }
        """.trimIndent()

        // Act
        val actual = try{
            OfferingParser.json.decodeFromString<PaywallComponent>(serialized)
            error("Should have thrown an exception")
        } catch (e: SerializationException) {
            e
        }

        // Assert
        assertThat(actual).isInstanceOf(SerializationException::class.java)
        assertThat(actual.message).isEqualTo("No fallback provided for unknown type: super_new_type")
    }

    @Test
    fun `Should fail to deserialize an unknown fallback`() {
        // Arrange
        // language=json
        val serialized = """
        {
          "type": "super_new_type",
          "unknown_property": {
            "type": "more_unknown"
          },
          "fallback": {
            "type": "less_new_but_still_new_type",
            "unknown_property": {
              "type": "more_unknown"
            }
          }
        }
        """.trimIndent()

        // Act
        val actual = try{
            OfferingParser.json.decodeFromString<PaywallComponent>(serialized)
            error("Should have thrown an exception")
        } catch (e: SerializationException) {
            e
        }

        // Assert
        assertThat(actual).isInstanceOf(SerializationException::class.java)
        assertThat(actual.message).isEqualTo("No fallback provided for unknown type: less_new_but_still_new_type")
    }

    @Test
    fun `Should fail to deserialize an invalid fallback`() {
        // Arrange
        // language=json
        val serialized = """
        {
          "type": "super_new_type",
          "unknown_property": {
            "type": "more_unknown"
          },
          "fallback": {
            "type": "text",
            "wrong": "property"
          }
        }
        """.trimIndent()

        // Act
        val actual = try{
            OfferingParser.json.decodeFromString<PaywallComponent>(serialized)
            error("Should have thrown an exception")
        } catch (e: SerializationException) {
            e
        }

        // Assert
        assertThat(actual).isInstanceOf(SerializationException::class.java)
        assertThat(actual.message).isEqualTo(
            "Fields [text_lid, color] are required for type with serial name 'text', but they were missing at path: $"
        )
    }

}
