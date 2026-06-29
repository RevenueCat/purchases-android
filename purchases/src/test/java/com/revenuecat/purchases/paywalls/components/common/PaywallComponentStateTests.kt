package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

@OptIn(InternalRevenueCatAPI::class)
internal class PaywallComponentStateTests {

    @Test
    fun `decodes a state_declarations map with all value types`() {
        @Language("json")
        val json = """
            {
              "planComparisonOpen": { "type": "boolean", "default": false },
              "activeSlide":        { "type": "integer", "default": 0 },
              "discountMultiplier": { "type": "double",  "default": 0.5 },
              "selectedFeatureTab": { "type": "string",  "default": "billing" }
            }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString(StateDeclarationMapSerializer, json)

        assertThat(actual).hasSize(4)
        assertThat(actual["planComparisonOpen"]?.type).isEqualTo(StateDeclaration.ValueType.BOOLEAN)
        assertThat(actual["planComparisonOpen"]?.defaultValue).isEqualTo(JsonPrimitive(false))
        assertThat(actual["activeSlide"]?.type).isEqualTo(StateDeclaration.ValueType.INTEGER)
        assertThat(actual["activeSlide"]?.defaultValue).isEqualTo(JsonPrimitive(0))
        assertThat(actual["discountMultiplier"]?.type).isEqualTo(StateDeclaration.ValueType.DOUBLE)
        assertThat(actual["discountMultiplier"]?.defaultValue).isEqualTo(JsonPrimitive(0.5))
        assertThat(actual["selectedFeatureTab"]?.type).isEqualTo(StateDeclaration.ValueType.STRING)
        assertThat(actual["selectedFeatureTab"]?.defaultValue).isEqualTo(JsonPrimitive("billing"))
    }

    @Test
    fun `ignores unknown fields in a state declaration`() {
        @Language("json")
        val json = """{ "k": { "type": "string", "default": "v", "future_field": 42 } }"""

        val actual = JsonTools.json.decodeFromString(StateDeclarationMapSerializer, json)

        assertThat(actual["k"]).isEqualTo(StateDeclaration(type = "string", defaultValue = JsonPrimitive("v")))
    }

    @Test
    fun `drops a malformed entry without failing the rest of the map`() {
        @Language("json")
        val json = """
            {
              "good": { "type": "string", "default": "v" },
              "bad":  { "type": "string", "default": { "nested": true } }
            }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString(StateDeclarationMapSerializer, json)

        assertThat(actual.keys).containsExactly("good")
    }

    @Test
    fun `decodes a non-object state_declarations value as an empty map`() {
        val actual = JsonTools.json.decodeFromString(StateDeclarationMapSerializer, """"not_an_object"""")

        assertThat(actual).isEmpty()
    }

    @Test
    fun `decodes a set update with each literal type`() {
        assertThat(decodeUpdate("""{ "set": "k", "to": "billing" }"""))
            .isEqualTo(StateUpdate.Set("k", StateUpdateValue.Literal(JsonPrimitive("billing"))))
        assertThat(decodeUpdate("""{ "set": "k", "to": 3 }"""))
            .isEqualTo(StateUpdate.Set("k", StateUpdateValue.Literal(JsonPrimitive(3))))
        assertThat(decodeUpdate("""{ "set": "k", "to": 0.5 }"""))
            .isEqualTo(StateUpdate.Set("k", StateUpdateValue.Literal(JsonPrimitive(0.5))))
        assertThat(decodeUpdate("""{ "set": "k", "to": true }"""))
            .isEqualTo(StateUpdate.Set("k", StateUpdateValue.Literal(JsonPrimitive(true))))
    }

    @Test
    fun `decodes a set update with the value payload reference`() {
        assertThat(decodeUpdate("""{ "set": "activeSlide", "to": "${'$'}value" }"""))
            .isEqualTo(StateUpdate.Set("activeSlide", StateUpdateValue.PayloadReference))
    }

    @Test
    fun `ignores unknown fields in a set update`() {
        assertThat(decodeUpdate("""{ "set": "k", "to": true, "op": "future" }"""))
            .isEqualTo(StateUpdate.Set("k", StateUpdateValue.Literal(JsonPrimitive(true))))
    }

    @Test
    fun `decodes unknown or malformed update shapes as Unsupported`() {
        assertThat(decodeUpdate("""{ "toggle": "k" }""")).isEqualTo(StateUpdate.Unsupported)
        assertThat(decodeUpdate("""{ "set": "k" }""")).isEqualTo(StateUpdate.Unsupported)
        assertThat(decodeUpdate("""{ "to": true }""")).isEqualTo(StateUpdate.Unsupported)
        assertThat(decodeUpdate("""{ "set": "k", "to": [1, 2] }""")).isEqualTo(StateUpdate.Unsupported)
        assertThat(decodeUpdate(""""not_an_object"""")).isEqualTo(StateUpdate.Unsupported)
    }

    @Test
    fun `degrades a malformed entry in a stateUpdates list without failing the others`() {
        @Language("json")
        val json = """
            [
              { "set": "a", "to": true },
              { "unknown": "shape" },
              { "set": "b", "to": "${'$'}value" }
            ]
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString<List<StateUpdate>>(json)

        assertThat(actual).containsExactly(
            StateUpdate.Set("a", StateUpdateValue.Literal(JsonPrimitive(true))),
            StateUpdate.Unsupported,
            StateUpdate.Set("b", StateUpdateValue.PayloadReference),
        )
    }

    @Test
    fun `decodes stateUpdates wired onto an interactive component`() {
        @Language("json")
        val json = """
            {
              "type": "button",
              "action": { "type": "restore_purchases" },
              "stack": { "type": "stack", "components": [] },
              "state_updates": [ { "set": "planComparisonOpen", "to": true } ]
            }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString<ButtonComponent>(json)

        assertThat(actual.stateUpdates).containsExactly(
            StateUpdate.Set("planComparisonOpen", StateUpdateValue.Literal(JsonPrimitive(true))),
        )
    }

    @Test
    fun `decodes a component without stateUpdates as null`() {
        @Language("json")
        val json = """
            {
              "type": "button",
              "action": { "type": "restore_purchases" },
              "stack": { "type": "stack", "components": [] }
            }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString<ButtonComponent>(json)

        assertThat(actual.stateUpdates).isNull()
    }

    private fun decodeUpdate(@Language("json") json: String): StateUpdate =
        JsonTools.json.decodeFromString(json)
}
