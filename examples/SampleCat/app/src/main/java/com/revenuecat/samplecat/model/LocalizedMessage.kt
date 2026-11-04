package com.revenuecat.samplecat.model

import androidx.annotation.StringRes

/**
 * Represents a localized message that can be formatted with arguments.
 *
 * @param resId The string resource ID
 * @param formatArgs Optional format arguments for the string
 */
data class LocalizedMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
)
