package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.VirtualCurrencyBalancesScreenViewState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

@Stable
internal class VirtualCurrencyBalancesScreenViewModel(
    private val purchases: PurchasesType,
) : ViewModel() {

    val viewState: StateFlow<VirtualCurrencyBalancesScreenViewState> = flow {
        emit(VirtualCurrencyBalancesScreenViewState.Loading)

        purchases.invalidateVirtualCurrenciesCache()
        try {
            val virtualCurrencies = purchases.awaitGetVirtualCurrencies()
            val sortedVirtualCurrencies = virtualCurrencies.all.values.sortedByDescending { it.balance }

            emit(VirtualCurrencyBalancesScreenViewState.Loaded(sortedVirtualCurrencies))
        } catch (@Suppress("SwallowedException") e: PurchasesException) {
            emit(VirtualCurrencyBalancesScreenViewState.Error(error = e.error))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VirtualCurrencyBalancesScreenViewState.Loading,
    )
}
