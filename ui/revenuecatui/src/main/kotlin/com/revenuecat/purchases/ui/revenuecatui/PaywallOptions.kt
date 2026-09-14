package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Stable
internal sealed class OfferingSelection {

    @Immutable
    data class OfferingType(val offeringType: Offering) : OfferingSelection()

    @Parcelize
    @Immutable
    data class IdAndPresentedOfferingContext(
        val offeringId: String,
        val presentedOfferingContext: PresentedOfferingContext?,
    ) : Parcelable, OfferingSelection()

    @Immutable
    object None : OfferingSelection()

    val offering: Offering?
        get() = when (this) {
            is OfferingType -> offeringType
            is IdAndPresentedOfferingContext -> null
            None -> null
        }

    val offeringIdentifier: String?
        get() = when (this) {
            is OfferingType -> offeringType.identifier
            is IdAndPresentedOfferingContext -> offeringId
            None -> null
        }
}

@Poko
@Immutable
public class PaywallOptions internal constructor(
    internal val offeringSelection: OfferingSelection,
    internal val shouldDisplayDismissButton: Boolean,
    public val fontProvider: FontProvider?,
    public val listener: PaywallListener?,
    public val purchaseLogic: PaywallPurchaseLogic?,
    internal val mode: PaywallMode,
    public val dismissRequest: () -> Unit,
    internal val dismissRequestWithExitOffering: DismissRequestWithExitOffering? = null,
    /**
     * Custom variables to be used in paywall text. These values will replace `{{ custom.key }}` or
     * `{{ $custom.key }}` placeholders in the paywall configuration.
     */
    public val customVariables: Map<String, CustomVariableValue> = emptyMap(),
    internal val injectedWorkflow: PublishedWorkflow? = null,
    internal val injectedWorkflowUiConfig: UiConfig = emptyUiConfig(),
    internal val injectedWorkflowOfferings: Offerings? = null,
) {
    public companion object {
        private const val hashMultiplier = 31
    }

    public constructor(builder: Builder) : this(
        offeringSelection = builder.offeringSelection,
        shouldDisplayDismissButton = builder.shouldDisplayDismissButton,
        fontProvider = builder.fontProvider,
        listener = builder.listener,
        purchaseLogic = builder.purchaseLogic,
        mode = builder.mode,
        dismissRequest = builder.dismissRequest,
        dismissRequestWithExitOffering = builder.dismissRequestWithExitOffering,
        customVariables = builder.customVariables,
        injectedWorkflow = builder.injectedWorkflow,
        injectedWorkflowUiConfig = builder.injectedWorkflowUiConfig,
        injectedWorkflowOfferings = builder.injectedWorkflowOfferings,
    )

    // Only key fields that affect the paywall's identity and rendering logic are used in hashCode.
    // Fields like fontProvider, listener, purchaseLogic, and dismissRequest are excluded because
    // they don't influence visual/structural uniqueness and may not be reliably hashable.
    override fun hashCode(): Int {
        var result = offeringSelection.offeringIdentifier.hashCode()
        result = hashMultiplier * result + shouldDisplayDismissButton.hashCode()
        result = hashMultiplier * result + mode.hashCode()
        result = hashMultiplier * result + customVariables.hashCode()
        result = hashMultiplier * result + injectedWorkflow.hashCode()
        result = hashMultiplier * result + injectedWorkflowUiConfig.hashCode()
        result = hashMultiplier * result + injectedWorkflowOfferings.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaywallOptions) return false

        return when {
            this.offeringSelection != other.offeringSelection -> false
            this.shouldDisplayDismissButton != other.shouldDisplayDismissButton -> false
            this.fontProvider != other.fontProvider -> false
            this.listener != other.listener -> false
            this.purchaseLogic != other.purchaseLogic -> false
            this.mode != other.mode -> false
            this.customVariables != other.customVariables -> false
            this.injectedWorkflow != other.injectedWorkflow -> false
            this.injectedWorkflowUiConfig != other.injectedWorkflowUiConfig -> false
            this.injectedWorkflowOfferings != other.injectedWorkflowOfferings -> false
            else -> this.dismissRequest == other.dismissRequest
        }
    }

    internal fun copy(
        offeringSelection: OfferingSelection = this.offeringSelection,
        shouldDisplayDismissButton: Boolean = this.shouldDisplayDismissButton,
        fontProvider: FontProvider? = this.fontProvider,
        listener: PaywallListener? = this.listener,
        purchaseLogic: PaywallPurchaseLogic? = this.purchaseLogic,
        mode: PaywallMode = this.mode,
        dismissRequest: () -> Unit = this.dismissRequest,
        dismissRequestWithExitOffering: DismissRequestWithExitOffering? = this.dismissRequestWithExitOffering,
        customVariables: Map<String, CustomVariableValue> = this.customVariables,
        injectedWorkflow: PublishedWorkflow? = this.injectedWorkflow,
        injectedWorkflowUiConfig: UiConfig = this.injectedWorkflowUiConfig,
        injectedWorkflowOfferings: Offerings? = this.injectedWorkflowOfferings,
    ): PaywallOptions = PaywallOptions(
        offeringSelection = offeringSelection,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
        fontProvider = fontProvider,
        listener = listener,
        purchaseLogic = purchaseLogic,
        mode = mode,
        dismissRequest = dismissRequest,
        dismissRequestWithExitOffering = dismissRequestWithExitOffering,
        customVariables = customVariables,
        injectedWorkflow = injectedWorkflow,
        injectedWorkflowUiConfig = injectedWorkflowUiConfig,
        injectedWorkflowOfferings = injectedWorkflowOfferings,
    )

    @Suppress("TooManyFunctions")
    public class Builder(
        internal val dismissRequest: () -> Unit,
    ) {
        internal var offeringSelection: OfferingSelection = OfferingSelection.None
        internal var shouldDisplayDismissButton: Boolean = false
        internal var fontProvider: FontProvider? = null
        internal var listener: PaywallListener? = null
        internal var purchaseLogic: PaywallPurchaseLogic? = null
        internal var mode: PaywallMode = PaywallMode.default
        internal var dismissRequestWithExitOffering: DismissRequestWithExitOffering? = null
        internal var customVariables: Map<String, CustomVariableValue> = emptyMap()
        internal var injectedWorkflow: PublishedWorkflow? = null
        internal var injectedWorkflowUiConfig: UiConfig = emptyUiConfig()
        internal var injectedWorkflowOfferings: Offerings? = null

        public fun setOffering(offering: Offering?): Builder = apply {
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        internal fun setOfferingIdAndPresentedOfferingContext(
            idAndPresentedOfferingContext: OfferingSelection.IdAndPresentedOfferingContext?,
        ) = apply {
            this.offeringSelection = idAndPresentedOfferingContext ?: OfferingSelection.None
        }

        internal fun setOfferingSelection(offeringSelection: OfferingSelection?) = apply {
            this.offeringSelection = offeringSelection ?: OfferingSelection.None
        }

        /**
         * Sets whether to display a close button on the paywall screen. Only available when using
         * [Paywall] and original template paywalls. Ignored when using [OriginalTemplatePaywallFooter] or
         * using v2 Paywalls. Defaults to false.
         */
        public fun setShouldDisplayDismissButton(shouldDisplayDismissButton: Boolean): Builder = apply {
            this.shouldDisplayDismissButton = shouldDisplayDismissButton
        }

        /**
         * Sets a font provider to provide the paywall with your custom fonts.
         * Only available for original template paywalls. Ignored for v2 Paywalls.
         */
        public fun setFontProvider(fontProvider: FontProvider?): Builder = apply {
            this.fontProvider = fontProvider
        }

        public fun setListener(listener: PaywallListener?): Builder = apply {
            this.listener = listener
        }

        public fun setPurchaseLogic(purchaseLogic: PaywallPurchaseLogic?): Builder = apply {
            this.purchaseLogic = purchaseLogic
        }

        internal fun setMode(mode: PaywallMode) = apply {
            this.mode = mode
        }

        internal fun setDismissRequestWithExitOffering(
            dismissRequestWithExitOffering: DismissRequestWithExitOffering?,
        ) = apply {
            this.dismissRequestWithExitOffering = dismissRequestWithExitOffering
        }

        /**
         * Sets custom variables to be used in paywall text. These values will replace
         * `{{ custom.key }}` or `{{ $custom.key }}` placeholders in the paywall configuration.
         *
         * @param variables A map of variable names to their [CustomVariableValue] values.
         */
        public fun setCustomVariables(variables: Map<String, CustomVariableValue>): Builder = apply {
            this.customVariables = CustomVariableKeyValidator.validateAndFilter(variables)
        }

        /**
         * Injects a pre-built workflow (multipage paywall) to render locally without fetching
         * it from the backend, together with the [Offering] it renders against. Internal
         * RevenueCat use only (e.g. mobile app preview).
         *
         * The workflow's steps resolve their offering from [offering] only; pass the single
         * offering the workflow references (prefer single-offering workflows in preview), or
         * null for workflows without an associated offering. This sets the offering for you,
         * so there's no need to also call [setOffering]. Optionally pass [uiConfig] to style the
         * injected workflow, since it no longer ships with its own `ui_config`.
         */
        @InternalRevenueCatAPI
        public fun injectedWorkflow(
            workflow: PublishedWorkflow,
            offering: Offering?,
            uiConfig: UiConfig = emptyUiConfig(),
        ): Builder = apply {
            injectedWorkflow(
                workflow,
                Offerings(current = offering, all = offering?.let { mapOf(it.identifier to it) }.orEmpty()),
                uiConfig,
            )
            this.offeringSelection = offering?.let { OfferingSelection.OfferingType(it) }
                ?: OfferingSelection.None
        }

        /**
         * Injects a pre-built workflow whose steps resolve their offering from [offerings] as they are reached, the
         * way a fetched workflow does. Leaves the offering selection alone, so no presented offering context is
         * stamped on the steps' offerings.
         */
        internal fun injectedWorkflow(
            workflow: PublishedWorkflow,
            offerings: Offerings,
            uiConfig: UiConfig,
        ): Builder = apply {
            this.injectedWorkflow = workflow
            this.injectedWorkflowOfferings = offerings
            this.injectedWorkflowUiConfig = uiConfig
        }

        public fun build(): PaywallOptions {
            return PaywallOptions(this)
        }
    }
}

/**
 * Internal dismissal channel that, unlike [PaywallOptions.dismissRequest], carries the exit offering to present
 * (if any), the result to deliver, and how the paywall was dismissed.
 */
internal typealias DismissRequestWithExitOffering =
    (exitOffering: Offering?, result: PaywallResult?, reason: PaywallDismissReason) -> Unit

/** How a paywall was dismissed, reported through [DismissRequestWithExitOffering]. */
internal enum class PaywallDismissReason {
    /** The user backed out: system back, or a navigate-back action on a workflow's first step. */
    NAVIGATED_BACK,

    /** Any other dismissal: a close action, a web checkout hand-off, or a completed purchase or restore. */
    CLOSE,
}

/**
 * An explicitly empty [UiConfig] for locally-injected workflows and previews that don't fetch a remote `ui_config`.
 * The remote fetch path no longer falls back to a default config, so this is only for in-memory construction.
 */
internal fun emptyUiConfig(): UiConfig =
    UiConfig(
        app = UiConfig.AppConfig(colors = emptyMap(), fonts = emptyMap()),
        localizations = emptyMap(),
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
    )
