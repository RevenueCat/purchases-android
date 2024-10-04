package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
) : ViewModel(), CustomerCenterViewModel {
    override val state: StateFlow<CustomerCenterState>
        get() = _state.asStateFlow()

    private val _state: MutableStateFlow<CustomerCenterState> = MutableStateFlow(CustomerCenterState.Loading)

    init {
        updateState()
    }

    private fun updateState() {
        viewModelScope.launch {
            try {
                val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
                _state.value = CustomerCenterState.Success(customerCenterConfigData.toString())
            } catch (e: PurchasesException) {
                _state.value = CustomerCenterState.Error(e.error)
            }
        }
    }
}
