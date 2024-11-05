package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.ActivityNotFoundException
import androidx.compose.ui.platform.UriHandler

/**
 * Opens the given [uri] in a browser. If no browser is installed, [fallbackAction] is called.
 */
@Suppress("SwallowedException")
fun UriHandler.openUriOrElse(uri: String, fallbackAction: (e: Exception) -> Unit) {
    try {
        openUri(uri)
    } catch (e: ActivityNotFoundException) {
        fallbackAction(e)
    } catch (e: IllegalArgumentException) {
        fallbackAction(e)
    }
}
