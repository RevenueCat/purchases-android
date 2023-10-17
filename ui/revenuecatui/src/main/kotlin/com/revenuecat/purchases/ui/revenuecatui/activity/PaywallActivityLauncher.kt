package com.revenuecat.purchases.ui.revenuecatui.activity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall

/**
 * Implement this interface to receive the result of the paywall activity.
 */
interface PaywallResultHandler : ActivityResultCallback<PaywallResult>

/**
 * Launches the paywall activity. You need to create this object during the activity's onCreate.
 * Then launch the activity at your moment of choice
 */
class PaywallActivityLauncher {

    private var activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>

    /**
     * Creates a new PaywallActivityLauncher from an activity.
     */
    constructor(activity: ComponentActivity, resultHandler: PaywallResultHandler) {
        activityResultLauncher = activity.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Creates a new PaywallActivityLauncher from a fragment.
     */
    constructor(fragment: Fragment, resultHandler: PaywallResultHandler) {
        activityResultLauncher = fragment.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Launch the paywall activity.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used.
     */
    @JvmOverloads
    fun launch(offering: Offering? = null, fontProvider: ParcelizableFontProvider? = null) {
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringId = offering?.identifier,
                fontProvider = fontProvider,
            ),
        )
    }

    /**
     * Launch the paywall activity if the current user does not have [requiredEntitlementIdentifier] active.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used.
     * @param requiredEntitlementIdentifier the paywall will be displayed only if the current user does not
     * have this entitlement active.
     */
    @JvmOverloads
    fun launchIfNeeded(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        requiredEntitlementIdentifier: String,
    ) {
        launchIfNeeded(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier),
        )
    }

    /**
     * Launch the paywall activity based on whether the result of [shouldDisplayBlock] is true.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used.
     * @param shouldDisplayBlock the paywall will be displayed only if this returns true.
     */
    @JvmOverloads
    fun launchIfNeeded(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayBlock: (CustomerInfo) -> Boolean,
    ) {
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            if (shouldDisplay) {
                activityResultLauncher.launch(
                    PaywallActivityArgs(
                        offeringId = offering?.identifier,
                        fontProvider = fontProvider,
                    ),
                )
            }
        }
    }
}
