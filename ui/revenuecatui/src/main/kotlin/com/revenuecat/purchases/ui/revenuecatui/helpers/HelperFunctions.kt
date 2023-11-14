package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.getCustomerInfoWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CompositionLocal containing the current Activity.
 * Needs to be provided, it's null by default.
 */
internal val LocalActivity: ProvidableCompositionLocal<Activity?> = compositionLocalOf { null }

@Composable
@ReadOnlyComposable
internal fun isInPreviewMode() = LocalInspectionMode.current

@Composable
@ReadOnlyComposable
internal fun windowAspectRatio(): Float {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.toFloat() / configuration.screenWidthDp
}

/**
 * Evaluates [shouldDisplayBlock] with the current CustomerInfo to determine if a paywall should be displayed.
 */
internal suspend fun shouldDisplayPaywall(shouldDisplayBlock: (CustomerInfo) -> Boolean): Boolean {
    return suspendCoroutine { continuation -> shouldDisplayPaywall(shouldDisplayBlock, continuation::resume) }
}

/**
 * Evaluates [shouldDisplayBlock] with the current CustomerInfo to determine if a paywall should be displayed.
 */
internal fun shouldDisplayPaywall(
    shouldDisplayBlock: (CustomerInfo) -> Boolean,
    result: (shouldDisplay: Boolean) -> Unit,
) {
    Purchases.sharedInstance.getCustomerInfoWith(
        onSuccess = {
            val shouldDisplay = shouldDisplayBlock(it)
            if (shouldDisplay) {
                Logger.d("Displaying paywall according to display logic")
            } else {
                Logger.d("Not displaying paywall according to display logic")
            }

            result(shouldDisplay)
        },
        onError = {
            Logger.e("Error fetching customer info to display paywall", PurchasesException(it))
            result(false)
        },
    )
}

/**
 * Creates a block to determine if a paywall should be displayed based on a CustomerInfo and entitlement identifier.
 */
internal fun shouldDisplayBlockForEntitlementIdentifier(entitlement: String): ((CustomerInfo) -> Boolean) {
    return { customerInfo ->
        customerInfo.entitlements[entitlement]?.isActive != true
    }
}
