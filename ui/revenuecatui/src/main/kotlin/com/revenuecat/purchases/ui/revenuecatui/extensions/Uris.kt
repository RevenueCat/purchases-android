package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/**
 * Opens the given [uri] in a browser. If the [uri] is invalid or no browser is installed, [fallbackAction] is called.
 *
 * This is an extension on Activity, since we need an Activity Context to avoid having to pass the
 * FLAG_ACTIVITY_NEW_TASK flag.
 */
@Suppress("SwallowedException")
fun Activity.openUriOrElse(uri: String, fallbackAction: (e: Exception) -> Unit) {
    val parsed = try {
        Uri.parse(uri)
    } catch (e: IllegalArgumentException) {
        fallbackAction(e)
        return
    }

    try {
        startActivity(Intent(Intent.ACTION_VIEW, parsed))
    } catch (e: ActivityNotFoundException) {
        fallbackAction(e)
        return
    }
}
