package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class ManageSubscriptionsViewModel(
    private val purchases: PurchasesType,
    screen: CustomerCenterConfigData.Screen,
    purchaseInformation: PurchaseInformation? = null,
) : ViewModel() {

    data class UiState(
        val screen: CustomerCenterConfigData.Screen,
        val purchaseInformation: PurchaseInformation?,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            screen = screen,
            purchaseInformation = purchaseInformation,
        ),
    )

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun determineFlow(path: CustomerCenterConfigData.HelpPath) {
        // Implement the logic to determine the flow for the given path
        if (path.type == CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE) {
            purchases.awaitRestore()
        }
    }
}
