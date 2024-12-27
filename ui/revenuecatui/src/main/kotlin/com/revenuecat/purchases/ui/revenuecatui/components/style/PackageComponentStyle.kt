package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package

@Immutable
internal class PackageComponentStyle(
    @get:JvmSynthetic
    val pkg: Package,
    @get:JvmSynthetic
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
) : ComponentStyle
