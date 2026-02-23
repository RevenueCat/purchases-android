@file:Suppress("DEPRECATION")

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
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayBlockForEntitlementIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldDisplayPaywall
import java.lang.ref.WeakReference

/**
 * Implement this interface to receive the result of the paywall activity.
 */
public interface PaywallResultHandler : ActivityResultCallback<PaywallResult>

/**
 * Implement this interface to receive whether the paywall was displayed when it depends on a condition.
 */
public interface PaywallDisplayCallback {
    public fun onPaywallDisplayResult(wasDisplayed: Boolean)
}

/**
 * Launches the paywall activity. You need to create this object during the activity's onCreate.
 * Then launch the activity at your moment of choice.
 * This can be instantiated with an [ActivityResultCaller] instance
 * like a [ComponentActivity] or a [Fragment].
 */
@Suppress("TooManyFunctions")
public class PaywallActivityLauncher(resultCaller: ActivityResultCaller, resultHandler: PaywallResultHandler) {
    private val activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>
    private var currentNonSerializableArgsKey: Int? = null

    // We need to know whether the activity is running or finished to avoid launching the paywall
    // after the activity has been destroyed. See https://github.com/RevenueCat/purchases-android/issues/1842.
    // We keep a weak reference to avoid memory leaks.
    private val weakActivity = WeakReference(resultCaller as? Activity)
    private val weakFragment = WeakReference(resultCaller as? Fragment)

    init {
        val wrappedHandler = object : PaywallResultHandler {
            override fun onActivityResult(result: PaywallResult) {
                currentNonSerializableArgsKey?.let {
                    PaywallActivityNonSerializableArgsStore.remove(it)
                    currentNonSerializableArgsKey = null
                }
                resultHandler.onActivityResult(result)
            }
        }
        activityResultLauncher = resultCaller.registerForActivityResult(PaywallContract(), wrappedHandler)
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
     * @param customVariables Custom variables to be used in paywall text. These values will replace
     * `{{ custom.key }}` or `{{ $custom.key }}` placeholders in the paywall configuration.
     */
    @JvmOverloads
    public fun launch(
        offering: Offering? = null,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
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
                customVariables = customVariables,
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
    public fun launch(
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

    @JvmName("launchWithOfferingId")
    @Suppress("LongParameterList")
    @InternalRevenueCatAPI
    @JvmOverloads
    @Deprecated(
        message = "Use launchWithOptions(PaywallActivityLaunchOptions) instead",
        replaceWith = ReplaceWith(
            "launchWithOptions(PaywallActivityLaunchOptions.Builder()" +
                ".setOfferingIdentifier(offeringIdentifier, presentedOfferingContext).build())",
        ),
    )
    public fun launch(
        offeringIdentifier: String,
        presentedOfferingContext: PresentedOfferingContext,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
    ) {
        @OptIn(InternalRevenueCatAPI::class)
        val options = PaywallActivityLaunchOptions.Builder()
            .setOfferingIdentifier(offeringIdentifier, presentedOfferingContext)
            .setFontProvider(fontProvider)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton)
            .setEdgeToEdge(edgeToEdge)
            .setCustomVariables(customVariables)
            .build()
        launchWithOptions(options)
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
    public fun launchIfNeeded(
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
    public fun launchIfNeeded(
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

    @JvmName("launchIfNeededWithOfferingId")
    @Suppress("LongParameterList")
    @InternalRevenueCatAPI
    @JvmOverloads
    @Deprecated(
        message = "Use launchIfNeededWithOptions(PaywallActivityLaunchIfNeededOptions) instead " +
            "for customVariables support",
        replaceWith = ReplaceWith(
            "launchIfNeededWithOptions(PaywallActivityLaunchIfNeededOptions.Builder()" +
                ".setRequiredEntitlementIdentifier(requiredEntitlementIdentifier).build())",
        ),
    )
    public fun launchIfNeeded(
        requiredEntitlementIdentifier: String,
        offeringIdentifier: String,
        presentedOfferingContext: PresentedOfferingContext,
        fontProvider: ParcelizableFontProvider? = null,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        paywallDisplayCallback: PaywallDisplayCallback? = null,
    ) {
        @OptIn(InternalRevenueCatAPI::class)
        val options = PaywallActivityLaunchIfNeededOptions.Builder()
            .setRequiredEntitlementIdentifier(requiredEntitlementIdentifier)
            .setOfferingIdentifier(offeringIdentifier, presentedOfferingContext)
            .setFontProvider(fontProvider)
            .setShouldDisplayDismissButton(shouldDisplayDismissButton)
            .setEdgeToEdge(edgeToEdge)
            .setPaywallDisplayCallback(paywallDisplayCallback)
            .build()
        launchIfNeededWithOptions(options)
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
    public fun launchIfNeeded(
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

    /**
     * Launch the paywall activity with the specified options.
     *
     * This method provides a builder-based API for launching paywalls with custom configuration.
     *
     * Example:
     * ```kotlin
     * val options = PaywallActivityLaunchOptions.Builder()
     *     .setOffering(offering)
     *     .build()
     *
     * launcher.launchWithOptions(options)
     * ```
     *
     * @param options The launch options configured via [PaywallActivityLaunchOptions.Builder]
     */
    public fun launchWithOptions(options: PaywallActivityLaunchOptions) {
        val nonSerializableArgsKey = storeNonSerializableArgsIfNeeded(
            options.purchaseLogic,
            options.listener,
        )
        activityResultLauncher.launch(
            PaywallActivityArgs(
                offeringIdAndPresentedOfferingContext = options.toOfferingSelection(),
                fontProvider = options.fontProvider,
                shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                edgeToEdge = options.edgeToEdge,
                customVariables = options.customVariables,
                nonSerializableArgsKey = nonSerializableArgsKey,
            ),
        )
    }

    /**
     * Launch the paywall activity conditionally with the specified options.
     *
     * The paywall will be displayed based on one of these conditions (exactly one must be set):
     * - [PaywallActivityLaunchIfNeededOptions.requiredEntitlementIdentifier]: Only show if user doesn't have this
     *   entitlement
     * - [PaywallActivityLaunchIfNeededOptions.shouldDisplayBlock]: Only show if this block returns true
     *
     * Example with entitlement check:
     * ```kotlin
     * val options = PaywallActivityLaunchIfNeededOptions.Builder()
     *     .setRequiredEntitlementIdentifier("premium")
     *     .setCustomVariables(mapOf("user_name" to CustomVariableValue.String("John")))
     *     .setPaywallDisplayCallback(object : PaywallDisplayCallback {
     *         override fun onPaywallDisplayResult(wasDisplayed: Boolean) {
     *             // Handle result
     *         }
     *     })
     *     .build()
     *
     * launcher.launchIfNeededWithOptions(options)
     * ```
     *
     * Example with custom condition:
     * ```kotlin
     * val options = PaywallActivityLaunchIfNeededOptions.Builder()
     *     .setShouldDisplayBlock { customerInfo ->
     *         customerInfo.entitlements.active.isEmpty()
     *     }
     *     .setCustomVariables(mapOf("user_name" to CustomVariableValue.String("John")))
     *     .build()
     *
     * launcher.launchIfNeededWithOptions(options)
     * ```
     *
     * @param options The launch options configured via [PaywallActivityLaunchIfNeededOptions.Builder].
     *                Must have either [PaywallActivityLaunchIfNeededOptions.Builder.setRequiredEntitlementIdentifier]
     *                or [PaywallActivityLaunchIfNeededOptions.Builder.setShouldDisplayBlock] set.
     */
    public fun launchIfNeededWithOptions(options: PaywallActivityLaunchIfNeededOptions) {
        val shouldDisplayBlock = if (options.requiredEntitlementIdentifier != null) {
            shouldDisplayBlockForEntitlementIdentifier(options.requiredEntitlementIdentifier)
        } else {
            // shouldDisplayBlock is guaranteed to be non-null by PaywallActivityLaunchIfNeededOptions.Builder
            options.shouldDisplayBlock!!
        }

        shouldDisplayPaywall(shouldDisplayBlock) { shouldDisplay ->
            options.paywallDisplayCallback?.onPaywallDisplayResult(shouldDisplay)
            if (shouldDisplay) {
                val nonSerializableArgsKey = storeNonSerializableArgsIfNeeded(
                    options.purchaseLogic,
                    options.listener,
                )
                launchPaywallWithArgs(
                    PaywallActivityArgs(
                        requiredEntitlementIdentifier = options.requiredEntitlementIdentifier,
                        offeringIdAndPresentedOfferingContext = options.toOfferingSelection(),
                        fontProvider = options.fontProvider,
                        shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                        edgeToEdge = options.edgeToEdge,
                        customVariables = options.customVariables,
                        nonSerializableArgsKey = nonSerializableArgsKey,
                    ),
                )
            }
        }
    }

    private fun storeNonSerializableArgsIfNeeded(
        purchaseLogic: PurchaseLogic?,
        listener: PaywallListener?,
    ): Int? {
        if (purchaseLogic == null && listener == null) return null
        val args = PaywallActivityNonSerializableArgs(
            purchaseLogic = purchaseLogic,
            listener = listener,
        )
        val key = PaywallActivityNonSerializableArgsStore.store(args)
        currentNonSerializableArgsKey = key
        return key
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
