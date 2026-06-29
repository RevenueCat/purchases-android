package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

@OptIn(InternalRevenueCatAPI::class)
internal class PaywallStateStoreTests {

    private fun declaration(type: String, default: JsonPrimitive) = StateDeclaration(type = type, defaultValue = default)

    private fun setUpdate(key: String, value: StateUpdateValue): StateUpdate = StateUpdate.Set(key, value)

    @Test
    fun `seeds current values and defaults from declarations`() {
        val store = PaywallStateStore(
            mapOf(
                "open" to declaration(StateDeclaration.ValueType.BOOLEAN, JsonPrimitive(false)),
                "tab" to declaration(StateDeclaration.ValueType.STRING, JsonPrimitive("billing")),
            ),
        )

        assertThat(store.values.toMap()).isEqualTo(mapOf("open" to JsonPrimitive(false), "tab" to JsonPrimitive("billing")))
        assertThat(store.defaults.toMap()).isEqualTo(store.values.toMap())
    }

    @Test
    fun `applies a set update with a literal value`() {
        val store = PaywallStateStore(mapOf("open" to declaration(StateDeclaration.ValueType.BOOLEAN, JsonPrimitive(false))))

        store.applyUpdates(listOf(setUpdate("open", StateUpdateValue.Literal(JsonPrimitive(true)))))

        assertThat(store.values["open"]).isEqualTo(JsonPrimitive(true))
    }

    @Test
    fun `substitutes the payload for a payload reference`() {
        val store = PaywallStateStore(mapOf("slide" to declaration(StateDeclaration.ValueType.INTEGER, JsonPrimitive(0))))

        store.applyUpdates(listOf(setUpdate("slide", StateUpdateValue.PayloadReference)), payload = JsonPrimitive(3))

        assertThat(store.values["slide"]).isEqualTo(JsonPrimitive(3))
    }

    @Test
    fun `ignores a payload reference when there is no payload`() {
        val store = PaywallStateStore(mapOf("slide" to declaration(StateDeclaration.ValueType.INTEGER, JsonPrimitive(0))))

        store.applyUpdates(listOf(setUpdate("slide", StateUpdateValue.PayloadReference)), payload = null)

        assertThat(store.values["slide"]).isEqualTo(JsonPrimitive(0))
    }

    @Test
    fun `ignores a write to an undeclared key`() {
        val store = PaywallStateStore(emptyMap())

        store.applyUpdates(listOf(setUpdate("ghost", StateUpdateValue.Literal(JsonPrimitive(true)))))

        assertThat(store.values).isEmpty()
    }

    @Test
    fun `ignores a write whose type does not match the declared type`() {
        val store = PaywallStateStore(mapOf("open" to declaration(StateDeclaration.ValueType.BOOLEAN, JsonPrimitive(false))))

        store.applyUpdates(listOf(setUpdate("open", StateUpdateValue.Literal(JsonPrimitive("true")))))

        assertThat(store.values["open"]).isEqualTo(JsonPrimitive(false))
    }

    @Test
    fun `accepts an integer write to a double-typed key`() {
        val store = PaywallStateStore(mapOf("ratio" to declaration(StateDeclaration.ValueType.DOUBLE, JsonPrimitive(0.0))))

        store.applyUpdates(listOf(setUpdate("ratio", StateUpdateValue.Literal(JsonPrimitive(2)))))

        assertThat(store.values["ratio"]).isEqualTo(JsonPrimitive(2))
    }

    @Test
    fun `ignores an unsupported update`() {
        val store = PaywallStateStore(mapOf("open" to declaration(StateDeclaration.ValueType.BOOLEAN, JsonPrimitive(false))))

        store.applyUpdates(listOf(StateUpdate.Unsupported))

        assertThat(store.values["open"]).isEqualTo(JsonPrimitive(false))
    }

    @Test
    fun `applies updates in declared order`() {
        val store = PaywallStateStore(mapOf("tab" to declaration(StateDeclaration.ValueType.STRING, JsonPrimitive("a"))))

        store.applyUpdates(
            listOf(
                setUpdate("tab", StateUpdateValue.Literal(JsonPrimitive("b"))),
                setUpdate("tab", StateUpdateValue.Literal(JsonPrimitive("c"))),
            ),
        )

        assertThat(store.values["tab"]).isEqualTo(JsonPrimitive("c"))
    }

    @Test
    fun `registerDeclarations adds new keys seeded from their defaults`() {
        val store = PaywallStateStore(mapOf("a" to declaration(StateDeclaration.ValueType.STRING, JsonPrimitive("x"))))

        store.registerDeclarations(mapOf("b" to declaration(StateDeclaration.ValueType.INTEGER, JsonPrimitive(7))))

        assertThat(store.values.toMap()).isEqualTo(mapOf("a" to JsonPrimitive("x"), "b" to JsonPrimitive(7)))
    }

    @Test
    fun `registerDeclarations does not reset a key that already has a value`() {
        // Mirrors workflow navigation: re-registering a screen's declarations must preserve state set elsewhere.
        val store = PaywallStateStore(mapOf("tab" to declaration(StateDeclaration.ValueType.STRING, JsonPrimitive("a"))))
        store.applyUpdates(listOf(setUpdate("tab", StateUpdateValue.Literal(JsonPrimitive("b")))))

        store.registerDeclarations(mapOf("tab" to declaration(StateDeclaration.ValueType.STRING, JsonPrimitive("a"))))

        assertThat(store.values["tab"]).isEqualTo(JsonPrimitive("b"))
    }

}
