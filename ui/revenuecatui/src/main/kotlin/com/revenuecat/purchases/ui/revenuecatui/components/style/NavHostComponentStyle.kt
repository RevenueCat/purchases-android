package com.revenuecat.purchases.ui.revenuecatui.components.style

import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles

internal data class NavHostComponentStyle(
    val startPage: String,
    val pages: Map<String, PageStyle>,
)

internal data class PageStyle(
    val stickyHeader: ComponentStyle?,
    val stack: ComponentStyle,
    val stickyFooter: ComponentStyle?,
    val background: BackgroundStyles,
)
