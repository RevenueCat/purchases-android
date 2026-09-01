package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import kotlinx.serialization.json.JsonObject

/** A snapshot for tests that care about the frames, not the payload. */
internal fun testContextSnapshot(
    customVariables: Map<String, CustomVariableValue> = emptyMap(),
): JsonObject = webViewContextSnapshot(
    customVariables = customVariables,
    locale = "en-US",
    darkMode = false,
)
