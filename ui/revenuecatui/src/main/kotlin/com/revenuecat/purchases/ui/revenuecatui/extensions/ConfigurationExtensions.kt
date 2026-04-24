package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsLayoutDirection

internal fun Configuration.rcLayoutDirection(): LayoutDirection {
    val locale = locales.get(0)
    return if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
}

internal fun Configuration.resolveLayoutDirection(
    editorLayoutDirection: PaywallComponentsLayoutDirection?,
    honorPreferredLocaleLayoutDirection: Boolean,
): LayoutDirection? =
    when (editorLayoutDirection) {
        PaywallComponentsLayoutDirection.RTL -> LayoutDirection.Rtl
        PaywallComponentsLayoutDirection.LTR -> LayoutDirection.Ltr
        PaywallComponentsLayoutDirection.LOCALE -> rcLayoutDirection()
        PaywallComponentsLayoutDirection.SYSTEM, null -> {
            if (honorPreferredLocaleLayoutDirection) rcLayoutDirection() else null
        }
    }

@Composable
internal fun ProvideLayoutDirection(
    layoutDirection: LayoutDirection?,
    content: @Composable () -> Unit,
) {
    if (layoutDirection != null) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            content()
        }
    } else {
        content()
    }
}
