package com.revenuecat.purchases.rules

/** Helpers shared by more than one operator, so none of them owns another. */
internal object RulesEngineUtils {

    /**
     * The one place the engine measures a string, in UTF-16 code units
     * (JS `String.length` parity).
     *
     * `rc.length` returns this and `rc.indexOf` reports positions through it,
     * so every length and position handed back to a rule is stated in the same
     * unit and a later change to that unit moves them together.
     */
    fun stringLength(string: String): Int = string.length
}
