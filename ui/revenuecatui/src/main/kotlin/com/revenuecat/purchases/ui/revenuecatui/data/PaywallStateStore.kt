package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * In-memory state store for state-driven paywalls, scoped to a single presentation session: one store per workflow
 * presentation (shared across its screens) or per standalone paywall. Seeded from the declared state defaults;
 * interactive components mutate it via [apply] and condition evaluation reads the current [values].
 *
 * State persists across screen navigation within a workflow and is never persisted across presentations.
 */
@OptIn(InternalRevenueCatAPI::class)
@Stable
internal class PaywallStateStore(declarations: Map<String, StateDeclaration>) {

    // Snapshot-backed so registration/mutations are both thread-safe (workflow pre-warm registers screens off the
    // main thread) and observable (dependent components recompose when state changes).
    private val declaredTypes = mutableStateMapOf<String, String>()
    private val declaredDefaults = mutableStateMapOf<String, JsonPrimitive>()
    private val currentValues = mutableStateMapOf<String, JsonPrimitive>()

    init {
        registerDeclarations(declarations)
    }

    /**
     * The declared default for each key, used as the fallback when a key has not been written.
     */
    val defaults: Map<String, JsonPrimitive> get() = declaredDefaults

    /**
     * The current value of each key. Reads are observable so dependent components recompose when a value changes.
     */
    val values: Map<String, JsonPrimitive> get() = currentValues

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
                currentValues[key] = declaration.defaultValue
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
                currentValues[setUpdate.key] = value
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
