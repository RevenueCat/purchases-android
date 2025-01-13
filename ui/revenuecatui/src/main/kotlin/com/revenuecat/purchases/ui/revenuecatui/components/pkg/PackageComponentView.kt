@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun PackageComponentView(
    style: PackageComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    StackComponentView(
        style = style.stackComponentStyle,
        state = state,
        // We act like a button, so we're handling the click already.
        clickHandler = { },
        modifier = modifier.clickable { state.update(selectedPackage = style.rcPackage) },
    )
}
