package com.revenuecat.purchases.ui.revenuecatui.helpers

internal object TestTag {
    const val PURCHASE_BUTTON_TAG = "PurchaseButton"

    fun selectButtonTestTag(packageId: String): String {
        return "SelectButton_$packageId"
    }
}
