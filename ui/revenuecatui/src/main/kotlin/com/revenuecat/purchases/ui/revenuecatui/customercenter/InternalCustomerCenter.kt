package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

@Composable
internal fun InternalCustomerCenter(
    viewModel: CustomerCenterViewModel = getCustomerCenterViewModel(),
) {
    CustomerCenterScaffold {
        when (val state = viewModel.state.collectAsState().value) {
            is CustomerCenterState.Loading -> CustomerCenterLoading()
            is CustomerCenterState.Error -> CustomerCenterError(state)
            is CustomerCenterState.Success -> CustomerCenterLoaded(state)
        }
    }
}

@Composable
private fun CustomerCenterScaffold(mainContent: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        mainContent()
    }
}

@Composable
private fun CustomerCenterLoading() {
    // CustomerCenter WIP: Add proper loading UI
    Text("Loading...")
}

@Composable
private fun CustomerCenterError(state: CustomerCenterState.Error) {
    // CustomerCenter WIP: Add proper error UI
    Text("Error: ${state.error}")
}

@Composable
private fun CustomerCenterLoaded(state: CustomerCenterState.Success) {
    // CustomerCenter WIP: Add proper success UI
    Column {
        Text("Customer Center config:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(state.customerCenterConfigDataString)
    }
}

@Composable
internal fun getCustomerCenterViewModel(
    purchases: PurchasesType = PurchasesImpl(),
    viewModel: CustomerCenterViewModel = viewModel<CustomerCenterViewModelImpl>(
        factory = CustomerCenterViewModelFactory(purchases),
    ),
): CustomerCenterViewModel {
    return viewModel
}

@Preview
@Composable
internal fun CustomerCenterLoadingPreview() {
    CustomerCenterScaffold {
        CustomerCenterLoading()
    }
}

@Preview
@Composable
internal fun CustomerCenterErrorPreview() {
    CustomerCenterScaffold {
        CustomerCenterError(CustomerCenterState.Error(PurchasesError(PurchasesErrorCode.UnknownBackendError)))
    }
}

@Preview
@Composable
internal fun CustomerCenterLoadedPreview() {
    val viewModel = getCustomerCenterViewModel(previewPurchases)
    InternalCustomerCenter(viewModel)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private val previewPurchases = object : PurchasesType {
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult {
        error("Not implemented for preview")
    }

    override suspend fun awaitRestore(): CustomerInfo {
        error("Not implemented for preview")
    }

    override suspend fun awaitOfferings(): Offerings {
        error("Not implemented for preview")
    }

    override suspend fun awaitCustomerInfo(fetchPolicy: CacheFetchPolicy): CustomerInfo {
        error("Not implemented for preview")
    }

    override suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData {
        return CustomerCenterConfigData(
            screens = mapOf(
                CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
                    title = "Manage Subscription",
                    subtitle = "Manage subscription subtitle",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "path-id-1",
                            title = "Subscription",
                            type = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                            promotionalOffer = null,
                            feedbackSurvey = null,
                        ),
                    ),
                ),
            ),
            appearance = CustomerCenterConfigData.Appearance(),
            localization = CustomerCenterConfigData.Localization(
                locale = "en_US",
                localizedStrings = mapOf(
                    "cancel" to "Cancel",
                    "subscription" to "Subscription",
                ),
            ),
            support = CustomerCenterConfigData.Support(email = "test@revenuecat.com"),
        )
    }

    override fun track(event: PaywallEvent) {
        error("Not implemented for preview")
    }

    override val purchasesAreCompletedBy: PurchasesAreCompletedBy
        get() = PurchasesAreCompletedBy.REVENUECAT

    override fun syncPurchases() {
        error("Not implemented for preview")
    }

    override val storefrontCountryCode: String
        get() = "US"
}
