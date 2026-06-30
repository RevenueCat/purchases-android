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

/**
 * In-memory state store for state-driven paywalls, scoped to a single presentation session: one store per workflow
 * presentation (shared across its screens) or per standalone paywall. Seeded from the declared state defaults;
 * interactive components mutate it via [applyUpdates] and condition evaluation reads via [currentValueOrDefault].
 *
 * State persists across screen navigation within a workflow and is never persisted across presentations.
 */
@OptIn(InternalRevenueCatAPI::class)
@Stable
internal class PaywallStateStore(declarations: Map<String, StateDeclaration>) {

    private val declaredTypes = ConcurrentHashMap<String, String>()
    private val declaredDefaults = mutableStateMapOf<String, JsonPrimitive>()

    // Per-key MutableState so mutating one key doesn't invalidate derivedStateOf blocks reading other keys.
    private val currentValues = mutableStateMapOf<String, MutableState<JsonPrimitive>>()

    init {
        registerDeclarations(declarations)
    }

    /**
     * Returns the current value for [key], falling back to the declared default if the key has never been written.
     * Reading this inside a `derivedStateOf` block subscribes only to [key]'s individual state, so a write to an
     * unrelated key does not invalidate the block.
     */
    fun currentValueOrDefault(key: String): JsonPrimitive? = currentValues[key]?.value ?: declaredDefaults[key]

    /**
     * Adds any keys from [declarations] not already known, seeding each with its declared default. Keys that already
     * exist keep their current value, so registering a workflow screen's declarations never resets state another
     * screen set.
     */
    fun registerDeclarations(declarations: Map<String, StateDeclaration>) {
        declarations.forEach { (key, declaration) ->
            if (key !in declaredDefaults) {
                declaredTypes[key] = declaration.type
                declaredDefaults[key] = declaration.defaultValue
                currentValues[key] = mutableStateOf(declaration.defaultValue)
            }
        }
    }

    /**
     * Applies [updates] in declared order. A `$value` reference resolves to [payload]; a reference with no payload is
     * skipped. Writes to undeclared keys, or values whose type does not match the declared type, are ignored.
     */
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

private fun JsonPrimitive.matchesDeclaredType(declaredType: String): Boolean = when (declaredType) {
    StateDeclaration.ValueType.BOOLEAN -> !isString && booleanOrNull != null
    StateDeclaration.ValueType.STRING -> isString
    StateDeclaration.ValueType.INTEGER -> !isString && longOrNull != null
    StateDeclaration.ValueType.DOUBLE -> !isString && doubleOrNull != null
    else -> false
}
