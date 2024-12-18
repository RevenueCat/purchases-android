package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
) : ViewModel(), CustomerCenterViewModel {
    companion object {
        private const val STOP_FLOW_TIMEOUT = 5_000L
    }

    // This won't load the state until there is a subscriber
    override val state = flow {
        try {
            val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
            emit(CustomerCenterState.Success(customerCenterConfigData))
        } catch (e: PurchasesException) {
            emit(CustomerCenterState.Error(e.error))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_FLOW_TIMEOUT),
        initialValue = CustomerCenterState.Loading,
    )
}
