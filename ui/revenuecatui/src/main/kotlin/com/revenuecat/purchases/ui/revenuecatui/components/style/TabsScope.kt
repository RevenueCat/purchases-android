package com.revenuecat.purchases.ui.revenuecatui.components.style

internal class TabsScope(val parent: TabSelection?, val sheetPresentationKey: SheetPresentationKey?) {
    var defaultTabIndex: Int = 0
}

internal data class TabSelection(val scope: TabsScope?, val index: Int)
