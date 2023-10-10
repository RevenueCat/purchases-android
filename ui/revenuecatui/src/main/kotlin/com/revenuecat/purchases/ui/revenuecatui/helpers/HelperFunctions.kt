package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalInspectionMode
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo

@Composable
@ReadOnlyComposable
internal fun isInPreviewMode() = LocalInspectionMode.current

/**
 * Evaluates [shouldDisplayBlock] with the current CustomerInfo to determine if a paywall should be displayed.
 */
internal suspend fun shouldDisplayPaywall(shouldDisplayBlock: (CustomerInfo) -> Boolean): Boolean {
    val shouldDisplayDialog = try {
        shouldDisplayBlock.invoke(Purchases.sharedInstance.awaitCustomerInfo())
    } catch (e: PurchasesException) {
        Logger.e("Error fetching customer info to display paywall", e)
        false
    }
    if (shouldDisplayDialog) {
        Logger.d("Displaying paywall according to display logic")
    } else {
        Logger.d("Not displaying paywall according to display logic")
    }

    return shouldDisplayDialog
}

/**
 * Creates a block to determine if a paywall should be displayed based on a CustomerInfo and entitlement identifier.
 */
internal fun shouldDisplayBlockForEntitlementIdentifier(entitlement: String): ((CustomerInfo) -> Boolean) {
    return { customerInfo ->
        customerInfo.entitlements[entitlement]?.isActive != true
    }
}
