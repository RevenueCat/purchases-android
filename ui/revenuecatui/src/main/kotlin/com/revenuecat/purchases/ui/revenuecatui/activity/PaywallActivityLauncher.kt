package com.revenuecat.purchases.ui.revenuecatui.activity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall

/**
 * Implement this interface to receive the result of the paywall activity.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
interface PaywallResultHandler : ActivityResultCallback<PaywallResult>

/**
 * Launches the paywall activity. You need to create this object during the activity's onCreate.
 * Then launch the activity at your moment of choice
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
class PaywallActivityLauncher {

    private var activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>

    /**
     * Creates a new PaywallActivityLauncher from an activity.
     */
    constructor(activity: ComponentActivity, resultHandler: PaywallResultHandler) :
        this(activity as ActivityResultCaller, resultHandler)

    /**
     * Creates a new PaywallActivityLauncher from a fragment.
     */
    constructor(fragment: Fragment, resultHandler: PaywallResultHandler) :
        this(fragment as ActivityResultCaller, resultHandler)

    /**
     * Creates a new PaywallActivityLauncher from an [ActivityResultCaller] instance.
     */
    constructor(resultCaller: ActivityResultCaller, resultHandler: PaywallResultHandler) {
        activityResultLauncher = resultCaller.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Launch the paywall activity.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall.
     */
    @JvmOverloads
    fun launch(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    ) {
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringId = offering?.identifier,
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
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
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall.
     */
    @JvmOverloads
    fun launchIfNeeded(
        requiredEntitlementIdentifier: String,
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    ) {
        launchIfNeeded(
            offering = offering,
            fontProvider = fontProvider,
            shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier),
            shouldDisplayDismissButton = shouldDisplayDismissButton,
        )
    }

    /**
     * Launch the paywall activity based on whether the result of [shouldDisplayBlock] is true.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall.
     * @param shouldDisplayBlock the paywall will be displayed only if this returns true.
     */
    @JvmOverloads
    fun launchIfNeeded(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        shouldDisplayBlock: (CustomerInfo) -> Boolean,
    ) {
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            if (shouldDisplay) {
                activityResultLauncher.launch(
                    PaywallActivityArgs(
                        offeringId = offering?.identifier,
                        fontProvider = fontProvider,
                        shouldDisplayDismissButton = shouldDisplayDismissButton,
                    ),
                )
            }
        }
    }
}
