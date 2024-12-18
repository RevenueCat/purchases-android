package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class ManageSubscriptionsViewModel(
    screen: CustomerCenterConfigData.Screen,
) : ViewModel() {

    data class UiState(
        val screen: CustomerCenterConfigData.Screen,
        val loadingPath: String? = null,
        val showRestoreAlert: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState(screen))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

} 