package com.revenuecat.purchases.ui.revenuecatui.mocks

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeViewModel(
    offering: Offering,
) : PaywallViewModel {
    override val resourceProvider: ResourceProvider
        get() = MockResourceProvider()
    override val state: StateFlow<PaywallState>
        get() = _state.asStateFlow()
    override val actionInProgress: State<Boolean>
        get() = _actionInProgress
    override val actionError: State<PurchasesError?>
        get() = _actionError

    private val _state = MutableStateFlow(
        offering.toPaywallState(
            variableDataProvider = VariableDataProvider(resourceProvider),
            activelySubscribedProductIdentifiers = setOf(),
            nonSubscriptionProductIdentifiers = setOf(),
            mode = PaywallMode.default,
            validatedPaywallData = offering.paywall!!,
            template = PaywallTemplate.fromId(offering.paywall!!.templateName)!!,
            shouldDisplayDismissButton = false,
        ),
    )
    private val _actionInProgress = mutableStateOf(false)
    private val _actionError = mutableStateOf<PurchasesError?>(null)

    var refreshStateIfLocaleChangedCallCount = 0
        private set
    override fun refreshStateIfLocaleChanged() {
        refreshStateIfLocaleChangedCallCount++
    }

    var refreshStateIfColorsChangedCallCount = 0
        private set
    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean) {
        refreshStateIfColorsChangedCallCount++
    }

    var selectPackageCallCount = 0
        private set
    var selectPackageCallParams = mutableListOf<TemplateConfiguration.PackageInfo>()
        private set
    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        selectPackageCallCount++
        selectPackageCallParams.add(packageToSelect)
    }

    var trackPaywallImpressionIfNeededCallCount = 0
        private set
    override fun trackPaywallImpressionIfNeeded() {
        trackPaywallImpressionIfNeededCallCount++
    }

    var closePaywallCallCount = 0
        private set
    override fun closePaywall() {
        closePaywallCallCount++
    }

    var purchaseSelectedPackageCallCount = 0
        private set
    var purchaseSelectedPackageParams = mutableListOf<Activity?>()
        private set
    override fun purchaseSelectedPackage(activity: Activity?) {
        purchaseSelectedPackageCallCount++
        purchaseSelectedPackageParams.add(activity)
    }

    var restorePurchasesCallCount = 0
        private set
    override fun restorePurchases() {
        restorePurchasesCallCount++
    }

    var clearActionErrorCallCount = 0
        private set
    override fun clearActionError() {
        clearActionErrorCallCount++
    }
}
