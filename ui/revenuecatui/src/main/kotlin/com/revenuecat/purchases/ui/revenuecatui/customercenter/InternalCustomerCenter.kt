@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.ManageSubscriptionsView
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.launch

@Composable
internal fun InternalCustomerCenter(
    modifier: Modifier = Modifier,
    viewModel: CustomerCenterViewModel = getCustomerCenterViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    InternalCustomerCenter(
        state,
        modifier,
        onDetermineFlow = { path ->
            coroutineScope.launch {
                viewModel.determineFlow(path)
            }
        },
    )
}

@Composable
private fun InternalCustomerCenter(
    state: CustomerCenterState,
    modifier: Modifier = Modifier,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    CustomerCenterScaffold(modifier) {
        when (state) {
            is CustomerCenterState.Loading -> CustomerCenterLoading()
            is CustomerCenterState.Error -> CustomerCenterError(state)
            is CustomerCenterState.Success -> CustomerCenterLoaded(state, onDetermineFlow)
        }
    }
}

@Composable
private fun CustomerCenterScaffold(
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun CustomerCenterLoaded(
    state: CustomerCenterState.Success,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    val configuration = state.customerCenterConfigData
    if (state.purchaseInformation != null) {
        configuration.getManagementScreen()?.let { managementScreen ->
            ManageSubscriptionsView(
                screen = managementScreen,
                purchaseInformation = state.purchaseInformation,
                onDetermineFlow = onDetermineFlow,
            )
        } ?: run {
            // Handle missing management screen
            // WrongPlatformView
        }
    } else {
        configuration.getNoActiveScreen()?.let { noActiveScreen ->
            ManageSubscriptionsView(
                screen = noActiveScreen,
                onDetermineFlow = onDetermineFlow,
            )
        } ?: run {
            // Fallback with a restore button
            // NoSubscriptionsView(configuration = configuration)
        }
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
    InternalCustomerCenter(
        state = CustomerCenterState.Loading,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
    )
}

@Preview
@Composable
internal fun CustomerCenterErrorPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Error(PurchasesError(PurchasesErrorCode.UnknownBackendError)),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview
@Composable
internal fun CustomerCenterLoadedPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Success(
            customerCenterConfigData = previewConfigData,
            purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private val previewConfigData = CustomerCenterConfigData(
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
