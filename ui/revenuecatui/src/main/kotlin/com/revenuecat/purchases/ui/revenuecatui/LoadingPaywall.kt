package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.composables.CloseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.DisableTouchesComposable
import com.revenuecat.purchases.ui.revenuecatui.composables.Fade
import com.revenuecat.purchases.ui.revenuecatui.composables.PlaceholderDefaults
import com.revenuecat.purchases.ui.revenuecatui.composables.placeholder
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.extensions.createDefault
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
internal fun LoadingPaywall(
    mode: PaywallMode,
    shouldDisplayDismissButton: Boolean,
    onDismiss: () -> Unit,
) {
    val resourceProvider = LocalContext.current.toResourceProvider()

    val paywallData: PaywallData = PaywallData.createDefault(
        LoadingPaywallConstants.packages,
        MaterialTheme.colorScheme,
        resourceProvider,
    )

    val offering = Offering(
        identifier = LoadingPaywallConstants.offeringIdentifier,
        serverDescription = "Loading paywall",
        metadata = emptyMap(),
        availablePackages = LoadingPaywallConstants.packages,
        paywall = paywallData,
    )

    val state = offering.toPaywallState(
        variableDataProvider = VariableDataProvider(
            resourceProvider,
            isInPreviewMode(),
        ),
        activelySubscribedProductIdentifiers = setOf(),
        nonSubscriptionProductIdentifiers = setOf(),
        mode = mode,
        validatedPaywallData = paywallData,
        template = LoadingPaywallConstants.template,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
    )

    when (state) {
        // The loading PaywallData is known at compile time
        // and snapshots ensure that these 2 states are impossible.
        is PaywallState.Error,
        is PaywallState.Loading,
        -> Box {}

        is PaywallState.Loaded -> LoadingPaywall(state, LoadingViewModel(state, resourceProvider), onDismiss)
    }
}

@Composable
private fun LoadingPaywall(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
) {
    Box {
        DisableTouchesComposable {
            // Template
            Template2(
                state = state,
                viewModel = viewModel,
                childModifier = Modifier
                    .placeholder(
                        visible = true,
                        shape = RoundedCornerShape(UIConstant.defaultPackageCornerRadius),
                        highlight = Fade(
                            highlightColor = LoadingPaywallConstants.placeholderColor,
                            animationSpec = PlaceholderDefaults.fadeAnimationSpec,
                        ),
                        color = LoadingPaywallConstants.placeholderColor,
                    ),
            )
        }
        CloseButton(
            shouldDisplayDismissButton = state.shouldDisplayDismissButton,
            actionInProgress = viewModel.actionInProgress.value,
            onClick = onDismiss,
        )
    }
}

private object LoadingPaywallConstants {
    const val offeringIdentifier = "loading_offering"

    const val placeholderAlpha = 0.5f
    val placeholderColor = Color.Gray.copy(alpha = placeholderAlpha)

    val template = PaywallTemplate.TEMPLATE_2

    val packages = listOf(
        Package(
            identifier = "weekly",
            packageType = PackageType.WEEKLY,
            offering = this.offeringIdentifier,
            product = TestStoreProduct(
                id = "com.revenuecat.weekly",
                name = "Weekly",
                title = "Weekly (App name)",
                price = Price(formatted = "$1.99", currencyCode = "USD", amountMicros = 1_990_000),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            ),
        ),
        Package(
            identifier = "monthly",
            packageType = PackageType.MONTHLY,
            offering = this.offeringIdentifier,
            product = TestStoreProduct(
                id = "com.revenuecat.monthly",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(formatted = "$5.99", currencyCode = "USD", amountMicros = 5_990_000),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        ),
        Package(
            identifier = "annual",
            packageType = PackageType.ANNUAL,
            offering = this.offeringIdentifier,
            product = TestStoreProduct(
                id = "com.revenuecat.annual",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(formatted = "$15.99", currencyCode = "USD", amountMicros = 5_990_000),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            ),
        ),
    )
}

private class LoadingViewModel(
    state: PaywallState,
    override val resourceProvider: ResourceProvider,
) : PaywallViewModel {
    override val state: StateFlow<PaywallState>
        get() = _state.asStateFlow()

    override val actionInProgress: State<Boolean> = mutableStateOf(false)
    override val actionError: State<PurchasesError?> = mutableStateOf(null)

    override fun trackPaywallImpressionIfNeeded() = Unit
    override fun refreshStateIfLocaleChanged() = Unit
    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDarkMode: Boolean) = Unit

    private val _state = MutableStateFlow(state)

    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        error("Not supported")
    }

    override fun closePaywall() {
        error("Not supported")
    }

    override fun purchaseSelectedPackage(activity: Activity?) {
        error("Can't purchase loading view model")
    }

    override fun restorePurchases() {
        error("Can't restore purchases")
    }

    override fun clearActionError() = Unit
}

@Preview(showBackground = true)
@Composable
internal fun LoadingPaywallPreview() {
    LoadingPaywall(
        mode = PaywallMode.FULL_SCREEN,
        shouldDisplayDismissButton = false,
        onDismiss = {},
    )
}
