package com.revenuecat.purchases.ui.debugview

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.debugview.models.InternalDebugRevenueCatScreenViewModel
import com.revenuecat.purchases.ui.debugview.models.InternalDebugRevenueCatScreenViewModelFactory
import com.revenuecat.purchases.ui.debugview.models.SettingGroupState
import com.revenuecat.purchases.ui.debugview.models.SettingScreenState
import com.revenuecat.purchases.ui.debugview.models.SettingState
import com.revenuecat.purchases.ui.debugview.settings.SettingGroup
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun InternalDebugRevenueCatScreen(
    onPurchaseCompleted: (StoreTransaction) -> Unit,
    onPurchaseErrored: (PurchasesTransactionException) -> Unit,
    // If screenViewModel is null, a default one will be created.
    screenViewModel: DebugRevenueCatViewModel? = null,
    activity: Activity? = null,
) {
    val viewModel = screenViewModel ?: viewModel<InternalDebugRevenueCatScreenViewModel>(
        factory = InternalDebugRevenueCatScreenViewModelFactory(onPurchaseCompleted, onPurchaseErrored),
    )
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        val currentActivity = activity ?: LocalContext.current.findActivity()
        val state = viewModel.state.collectAsState().value
        DisplayToastMessageIfNeeded(viewModel, state = state)
        Text(
            text = "RevenueCat Debug Menu",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        state.toSettingGroupStates().forEach { SettingGroup(it, viewModel, currentActivity) }
    }
}

@Composable
private fun DisplayToastMessageIfNeeded(viewModel: DebugRevenueCatViewModel, state: SettingScreenState) {
    state.toastMessage?.let { toastMessage ->
        Toast.makeText(
            LocalContext.current,
            toastMessage,
            Toast.LENGTH_LONG,
        ).show()
        viewModel.toastDisplayed()
    }
}

@Preview(showBackground = true)
@Composable
private fun InternalDebugRevenueCatScreenPreview() {
    InternalDebugRevenueCatScreen(
        onPurchaseCompleted = {},
        onPurchaseErrored = {},
        screenViewModel = object : DebugRevenueCatViewModel {
            override val state = MutableStateFlow<SettingScreenState>(
                SettingScreenState.Configured(
                    SettingGroupState(
                        "Configuration",
                        listOf(
                            SettingState.Text("SDK version", "3.0.0"),
                            SettingState.Text("Observer mode", "true"),
                        ),
                    ),
                    SettingGroupState(
                        "Customer info",
                        listOf(
                            SettingState.Text("Current User ID", "current-user-id"),
                            SettingState.Text("Active entitlements", "pro, premium"),
                        ),
                    ),
                    SettingGroupState(
                        "Offerings",
                        listOf(
                            SettingState.Text("current", "TODO"),
                            SettingState.Text("default", "TODO"),
                        ),
                    ),
                ),
            )

            override fun toastDisplayed() {
                error("Not implemented")
            }

            override fun purchasePackage(activity: Activity, rcPackage: Package) {
                error("Not implemented")
            }

            override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
                error("Not implemented")
            }

            override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
                error("Not implemented")
            }
        },
    )
}
