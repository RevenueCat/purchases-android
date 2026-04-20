package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the given [uri] in a browser. If the [uri] is invalid or no browser is installed, [fallbackAction] is called.
 */
public fun Context.openUriOrElse(uri: String, fallbackAction: (e: Exception) -> Unit) {
    val parsed = runCatching { Uri.parse(uri) }
        .getOrElse { throwable ->
            if (throwable is IllegalArgumentException) {
                fallbackAction(throwable)
                return
            }
            throw throwable
        }

    runCatching { startActivity(Intent(Intent.ACTION_VIEW, parsed)) }
        .onFailure { throwable ->
            if (throwable is ActivityNotFoundException) {
                fallbackAction(throwable)
            } else {
                throw throwable
            }
        }
}
