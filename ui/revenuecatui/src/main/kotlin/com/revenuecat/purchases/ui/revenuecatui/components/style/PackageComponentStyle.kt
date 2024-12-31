package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal class PackageComponentStyle(
    @get:JvmSynthetic
    val pkg: Package,
    @get:JvmSynthetic
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
) : ComponentStyle {
    override val size: Size = stackComponentStyle.size
}
