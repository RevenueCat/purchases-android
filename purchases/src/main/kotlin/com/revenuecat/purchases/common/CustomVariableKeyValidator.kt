package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * The naming policy for developer-supplied custom variable keys, shared by paywall custom variables and the
 * `custom.*` dimensions local rule evaluation reads.
 *
 * Valid keys:
 * - Must not be empty
 * - Can only contain letters, numbers, and underscores
 *
 * The underscore-only rule is also what makes a key addressable from a rule predicate: `var` walks nested objects
 * by dot-path, so a key containing a `.` could never be read.
 */
@InternalRevenueCatAPI
public object CustomVariableKeyValidator {

    @Suppress("ReturnCount")
    public fun isValidKey(key: String): Boolean {
        if (key.isEmpty()) return false
        var index = 0
        while (index < key.length) {
            val codePoint = key.codePointAt(index)
            if (!codePoint.isValidKeyCodePoint()) return false
            index += Character.charCount(codePoint)
        }
        return true
    }

    /**
     * Returns [variables] without the entries whose key is invalid, logging a warning for each one dropped.
     */
    public fun <T> validateAndFilter(variables: Map<String, T>): Map<String, T> =
        variables.filter { (key, _) ->
            isValidKey(key).also { valid ->
                if (!valid) {
                    warnLog {
                        "Custom variable key '$key' is invalid and will be ignored. " +
                            "Keys must not be empty and contain only letters, numbers, and underscores."
                    }
                }
            }
        }

    private fun Int.isValidKeyCodePoint(): Boolean =
        Character.isAlphabetic(this) ||
            Character.getType(this) == Character.DECIMAL_DIGIT_NUMBER.toInt() ||
            this == '_'.code
}
