package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import kotlinx.serialization.json.JsonObject

/** A WebView may only be created and driven from the main thread. */
internal fun onMain(block: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

internal const val TEST_BUNDLE_URL = "https://assets.example.com/promo/index.html"

internal fun deviceContextSnapshot(
    customVariables: Map<String, CustomVariableValue> = emptyMap(),
): JsonObject = webViewContextSnapshot(
    WebViewContextInput(
        customVariables = customVariables,
        offering = null,
        componentPackage = null,
        selectedPackage = null,
        store = Store.PLAY_STORE,
        storefrontCountryCode = "US",
        locale = "en-US",
        darkMode = false,
    ),
)
