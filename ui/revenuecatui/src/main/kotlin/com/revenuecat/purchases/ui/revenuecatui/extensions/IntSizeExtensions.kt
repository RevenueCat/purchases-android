package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode

internal val IntSize.aspectRatio: Float
    get() = width.toFloat() / height

internal val IntSize.isLandscape: Boolean
    get() = width > height

/**
 * @return: Modifier with a callback indicating whether landscape layout should be used.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
internal fun PaywallState.Loaded.onLandscapeLayoutChanged(
    changed: (Boolean) -> Unit,
) = Modifier
    .then(
        // TODO: vertical compact?
        Modifier.onGloballyPositioned { changed(this.isInFullScreenMode && it.size.isLandscape) },
    )
