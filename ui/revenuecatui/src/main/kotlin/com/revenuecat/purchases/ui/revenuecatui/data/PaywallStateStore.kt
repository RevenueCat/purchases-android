package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.concurrent.ConcurrentHashMap

/** In-memory state store for state-driven paywalls, scoped to one presentation session. */
@OptIn(InternalRevenueCatAPI::class)
@Stable
internal class PaywallStateStore(declarations: Map<String, StateDeclaration>) {

    private val declaredTypes = ConcurrentHashMap<String, StateDeclaration.ValueType>()
    private val declaredDefaults = mutableStateMapOf<String, JsonPrimitive>()

    // Per-key MutableState so mutating one key doesn't invalidate derivedStateOf blocks reading other keys.
    private val currentValues = mutableStateMapOf<String, MutableState<JsonPrimitive>>()

    init {
        registerDeclarations(declarations)
    }

    fun currentValueOrDefault(key: String): JsonPrimitive? = currentValues[key]?.value ?: declaredDefaults[key]

    /**
     * Adds new keys from [declarations]; keys already in the store keep their current value.
     * Synchronized: workflow prewarm (background) and navigation (main) can hit the same store.
     */
    @Synchronized
    fun registerDeclarations(declarations: Map<String, StateDeclaration>) {
        declarations.forEach { (key, declaration) ->
            if (key !in declaredDefaults) {
                declaredTypes[key] = declaration.type
                declaredDefaults[key] = declaration.defaultValue
                currentValues[key] = mutableStateOf(declaration.defaultValue)
            }
        }
    }

    @Synchronized
    fun applyUpdates(updates: List<StateUpdate>, payload: JsonPrimitive? = null) {
        updates.forEach { update ->
            val setUpdate = update as? StateUpdate.Set ?: return@forEach
            val value = when (val updateValue = setUpdate.value) {
                is StateUpdateValue.Literal -> updateValue.value
                StateUpdateValue.PayloadReference -> payload ?: return@forEach
            }
            val declaredType = declaredTypes[setUpdate.key] ?: return@forEach
            if (value.matchesDeclaredType(declaredType)) {
                // Mutate .value, not the map entry: reassigning the entry would invalidate all-key readers.
                currentValues[setUpdate.key]?.value = value
            }
        }
    }
}

private fun JsonPrimitive.matchesDeclaredType(declaredType: StateDeclaration.ValueType): Boolean =
    when (declaredType) {
        StateDeclaration.ValueType.BOOLEAN -> !isString && booleanOrNull != null
        StateDeclaration.ValueType.STRING -> isString
        StateDeclaration.ValueType.INTEGER -> !isString && longOrNull != null
        StateDeclaration.ValueType.DOUBLE -> !isString && doubleOrNull != null
        StateDeclaration.ValueType.UNKNOWN -> false
    }
