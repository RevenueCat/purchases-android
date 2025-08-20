package com.revenuecat.purchases.ui.revenuecatui.activity

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import java.lang.ref.WeakReference

/**
 * Implement this interface to receive the result of the paywall activity.
 */
interface PaywallResultHandler : ActivityResultCallback<PaywallResult>

/**
 * Implement this interface to receive whether the paywall was displayed when it depends on a condition.
 */
interface PaywallDisplayCallback {
    fun onPaywallDisplayResult(wasDisplayed: Boolean)
}

/**
 * Launches the paywall activity. You need to create this object during the activity's onCreate.
 * Then launch the activity at your moment of choice.
 * This can be instantiated with an [ActivityResultCaller] instance
 * like a [ComponentActivity] or a [Fragment].
 */
class PaywallActivityLauncher(resultCaller: ActivityResultCaller, resultHandler: PaywallResultHandler) {
    private val activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>

    // We need to know whether the activity is running or finished to avoid launching the paywall
    // after the activity has been destroyed. See https://github.com/RevenueCat/purchases-android/issues/1842.
    // We keep a weak reference to avoid memory leaks.
    private val weakActivity = WeakReference(resultCaller as? Activity)
    private val weakFragment = WeakReference(resultCaller as? Fragment)

    init {
        activityResultLauncher = resultCaller.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Launch the paywall activity.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used. Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall.
     * Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param edgeToEdge Whether to display the paywall in edge-to-edge mode.
     * Default is true for Android 15+, false otherwise.
     */
    @JvmOverloads
    fun launch(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
    ) {
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringIdAndPresentedOfferingContext = offering?.let {
                    OfferingSelection.IdAndPresentedOfferingContext(
                        offeringId = it.identifier,
                        presentedOfferingContext = it.availablePackages.firstOrNull()?.presentedOfferingContext,
                    )
                },
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
            ),
        )
    }

    /**
     * Do not use this method, use the method with the same name that takes an [Offering] instead.
     * This method is used internally by the hybrid SDKs.
     *
     * Launch the paywall activity.
     * @param offeringIdentifier The offering identifier of the offering to be shown in the paywall. If null, the
     * current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used. Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall. Only available for
     * original template paywalls. Ignored for v2 Paywalls.
     * @param edgeToEdge Whether to display the paywall in edge-to-edge mode.
     * Default is true for Android 15+, false otherwise.
     */
    @Deprecated(
        message = "Use launch with offering instead",
        replaceWith = ReplaceWith(
            expression = "launch(offering, fontProvider, shouldDisplayDismissButton, edgeToEdge)",
            imports = ["com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher"],
        ),
    )
    @JvmSynthetic
    fun launch(
        offeringIdentifier: String,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
    ) {
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                    offeringId = offeringIdentifier,
                    presentedOfferingContext = null,
                ),
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
            ),
        )
    }

    @InternalRevenueCatAPI
    @JvmSynthetic
    fun launch(
        offeringIdentifier: String,
        presentedOfferingContext: PresentedOfferingContext,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
    ) {
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                    offeringId = offeringIdentifier,
                    presentedOfferingContext = presentedOfferingContext,
                ),
                fontProvider = fontProvider,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
                edgeToEdge = edgeToEdge,
            ),
        )
    }

    /**
     * Launch the paywall activity if the current user does not have [requiredEntitlementIdentifier] active.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used. Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param requiredEntitlementIdentifier the paywall will be displayed only if the current user does not
     * have this entitlement active.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall. Only available for
     * original template paywalls. Ignored for v2 Paywalls.
     * @param edgeToEdge Whether to display the paywall in edge-to-edge mode.
     * Default is true for Android 15+, false otherwise.
     * @param paywallDisplayCallback Callback that will be called with true if the paywall was displayed
     */
    @Suppress("LongParameterList")
    @JvmOverloads
    fun launchIfNeeded(
        requiredEntitlementIdentifier: String,
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        paywallDisplayCallback: PaywallDisplayCallback? = null,
    ) {
        val shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            paywallDisplayCallback?.onPaywallDisplayResult(shouldDisplay)
            if (shouldDisplay) {
                launchPaywallWithArgs(
                    PaywallActivityArgs(
                        requiredEntitlementIdentifier = requiredEntitlementIdentifier,
                        offeringIdAndPresentedOfferingContext = offering?.let {
                            OfferingSelection.IdAndPresentedOfferingContext(
                                offeringId = it.identifier,
                                presentedOfferingContext = it.availablePackages.firstOrNull()?.presentedOfferingContext,
                            )
                        },
                        fontProvider = fontProvider,
                        shouldDisplayDismissButton = shouldDisplayDismissButton,
                        edgeToEdge = edgeToEdge,
                    ),
                )
            }
        }
    }

    /**
     * Do not use this method, use the method with the same name that takes an [Offering] instead.
     * This method is used internally by the hybrid SDKs.
     *
     * Launch the paywall activity if the current user does not have [requiredEntitlementIdentifier] active.
     * @param offeringIdentifier The offering identifier of the ofering to be shown in the paywall. If null, the
     * current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used. Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param requiredEntitlementIdentifier the paywall will be displayed only if the current user does not
     * have this entitlement active.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall. Only available for
     * original template paywalls. Ignored for v2 Paywalls.
     * @param edgeToEdge Whether to display the paywall in edge-to-edge mode.
     * Default is true for Android 15+, false otherwise.
     * @param paywallDisplayCallback Callback that will be called with true if the paywall was displayed
     */
    @Deprecated(
        message = "Use launchIfNeeded with offering instead",
        replaceWith = ReplaceWith(
            expression = "launchIfNeeded(" +
                "requiredEntitlementIdentifier, offering, fontProvider, " +
                "shouldDisplayDismissButton, edgeToEdge, paywallDisplayCallback" +
                ")",
            imports = ["com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher"],
        ),
    )
    @Suppress("LongParameterList")
    @JvmSynthetic
    fun launchIfNeeded(
        requiredEntitlementIdentifier: String,
        offeringIdentifier: String,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        paywallDisplayCallback: PaywallDisplayCallback? = null,
    ) {
        val shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            paywallDisplayCallback?.onPaywallDisplayResult(shouldDisplay)
            if (shouldDisplay) {
                launchPaywallWithArgs(
                    PaywallActivityArgs(
                        requiredEntitlementIdentifier = requiredEntitlementIdentifier,
                        offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                            offeringId = offeringIdentifier,
                            presentedOfferingContext = null,
                        ),
                        fontProvider = fontProvider,
                        shouldDisplayDismissButton = shouldDisplayDismissButton,
                        edgeToEdge = edgeToEdge,
                    ),
                )
            }
        }
    }

    @Suppress("LongParameterList")
    @InternalRevenueCatAPI
    @JvmSynthetic
    fun launchIfNeeded(
        requiredEntitlementIdentifier: String,
        offeringIdentifier: String,
        presentedOfferingContext: PresentedOfferingContext,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        paywallDisplayCallback: PaywallDisplayCallback? = null,
    ) {
        val shouldDisplayBlock = shouldDisplayBlockForEntitlementIdentifier(requiredEntitlementIdentifier)
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            paywallDisplayCallback?.onPaywallDisplayResult(shouldDisplay)
            if (shouldDisplay) {
                launchPaywallWithArgs(
                    PaywallActivityArgs(
                        requiredEntitlementIdentifier = requiredEntitlementIdentifier,
                        offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                            offeringId = offeringIdentifier,
                            presentedOfferingContext = presentedOfferingContext,
                        ),
                        fontProvider = fontProvider,
                        shouldDisplayDismissButton = shouldDisplayDismissButton,
                        edgeToEdge = edgeToEdge,
                    ),
                )
            }
        }
    }

    /**
     * Launch the paywall activity based on whether the result of [shouldDisplayBlock] is true.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     * @param fontProvider The [ParcelizableFontProvider] to be used in the paywall. If null, the default fonts
     * will be used. Only available for original template paywalls. Ignored for v2 Paywalls.
     * @param shouldDisplayDismissButton Whether to display the dismiss button in the paywall. Only available for
     * original template paywalls. Ignored for v2 Paywalls.
     * @param edgeToEdge Whether to display the paywall in edge-to-edge mode.
     * Default is true for Android 15+, false otherwise.
     * @param shouldDisplayBlock the paywall will be displayed only if this returns true.
     */
    @JvmOverloads
    fun launchIfNeeded(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        shouldDisplayBlock: (CustomerInfo) -> Boolean,
    ) {
        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            if (shouldDisplay) {
                launchPaywallWithArgs(
                    PaywallActivityArgs(
                        offeringIdAndPresentedOfferingContext = offering?.let {
                            OfferingSelection.IdAndPresentedOfferingContext(
                                offeringId = it.identifier,
                                presentedOfferingContext = it.availablePackages.firstOrNull()?.presentedOfferingContext,
                            )
                        },
                        fontProvider = fontProvider,
                        shouldDisplayDismissButton = shouldDisplayDismissButton,
                        edgeToEdge = edgeToEdge,
                    ),
                )
            }
        }
    }

    private fun launchPaywallWithArgs(args: PaywallActivityArgs) {
        if (isActivityFinishing()) {
            Logger.e("Not displaying paywall because activity/fragment is finishing or has finished.")
            return
        }
        activityResultLauncher.launch(args)
    }

    private fun isActivityFinishing(): Boolean {
        val activity = weakActivity.get()
        val fragment = weakFragment.get()
        return (activity == null && fragment?.activity == null) ||
            activity?.isFinishing == true ||
            fragment?.activity?.isFinishing == true
    }
}
