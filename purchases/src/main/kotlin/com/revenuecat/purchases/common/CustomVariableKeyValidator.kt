package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * The naming policy for developer-supplied custom variable keys, shared by paywall custom variables and the
 * `custom.*` dimensions local rule evaluation reads.
 *
 * Valid keys:
 * - Must not be empty
 * - Must start with a letter
 * - Can only contain letters, numbers, and underscores
 *
 * The underscore-only rule is also what makes a key addressable from a rule predicate: `var` walks nested objects
 * by dot-path, so a key containing a `.` could never be read.
 */
@InternalRevenueCatAPI
public object CustomVariableKeyValidator {

    public fun isValidKey(key: String): Boolean =
        key.isNotEmpty() &&
            key.first().isLetter() &&
            key.all { it.isLetter() || it.isDigit() || it == '_' }

    /**
     * Returns [variables] without the entries whose key is invalid, logging a warning for each one dropped.
     */
    public fun <T> validateAndFilter(variables: Map<String, T>): Map<String, T> =
        variables.filter { (key, _) ->
            isValidKey(key).also { valid ->
                if (!valid) {
                    warnLog {
                        "Custom variable key '$key' is invalid and will be ignored. " +
                            "Keys must start with a letter and contain only letters, numbers, and underscores."
                    }
                }
            }
        }
}
