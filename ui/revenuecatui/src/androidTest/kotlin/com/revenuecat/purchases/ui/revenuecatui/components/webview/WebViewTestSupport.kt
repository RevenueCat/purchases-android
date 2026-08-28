package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.test.platform.app.InstrumentationRegistry

/** A WebView may only be created and driven from the main thread. */
internal fun onMain(block: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

internal const val TEST_BUNDLE_URL = "https://assets.example.com/promo/index.html"
