package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.VirtualCurrencyBalancesScreenViewState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Stable
internal class VirtualCurrencyBalancesScreenViewModel(
    private val purchases: PurchasesType
) : ViewModel() {

    private val _viewState = MutableStateFlow<VirtualCurrencyBalancesScreenViewState>(VirtualCurrencyBalancesScreenViewState.Loading)
    val viewState: StateFlow<VirtualCurrencyBalancesScreenViewState> = _viewState.asStateFlow()

    fun onViewAppeared() {
        viewModelScope.launch {
            _viewState.value = VirtualCurrencyBalancesScreenViewState.Loading
            try {
                val virtualCurrencies = purchases.awaitGetVirtualCurrencies()
                val sortedCurrencies = virtualCurrencies.all.values.sortedByDescending { it.balance }
                _viewState.value = VirtualCurrencyBalancesScreenViewState.Loaded(sortedCurrencies)
            } catch (e: Exception) {
                _viewState.value = VirtualCurrencyBalancesScreenViewState.Error
            }
        }
    }
}
