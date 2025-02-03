@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.extensions.openUriOrElse
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

internal enum class URLOpeningMethod {
    @JvmSynthetic IN_APP_BROWSER,

    @JvmSynthetic EXTERNAL_BROWSER,

    @JvmSynthetic DEEP_LINK,
}

internal object URLOpener {
    @JvmSynthetic internal fun openURL(context: Context, url: String, method: URLOpeningMethod) {
        fun handleException(exception: Exception) {
            val message = if (exception is ActivityNotFoundException) {
                context.getString(R.string.no_browser_cannot_open_link)
            } else {
                context.getString(R.string.cannot_open_link)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            Logger.e(message, exception)
        }

        when (method) {
            URLOpeningMethod.IN_APP_BROWSER -> {
                val intent = CustomTabsIntent.Builder()
                    .build()
                @Suppress("TooGenericExceptionCaught")
                try {
                    intent.launchUrl(context, Uri.parse(url))
                } catch (e: ActivityNotFoundException) {
                    handleException(e)
                } catch (e: IllegalArgumentException) {
                    handleException(e)
                }
            }

            URLOpeningMethod.EXTERNAL_BROWSER,
            URLOpeningMethod.DEEP_LINK,
            -> context.openUriOrElse(url, ::handleException)
        }
    }
}
