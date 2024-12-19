package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
    suspend fun determineFlow(path: CustomerCenterConfigData.HelpPath)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
) : ViewModel(), CustomerCenterViewModel {
    companion object {
        private const val STOP_FLOW_TIMEOUT = 5_000L
    }

    override val state = flow {
        try {
            val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
            val purchaseInformation = loadPurchaseInformation()
            emit(CustomerCenterState.Success(customerCenterConfigData, purchaseInformation))
        } catch (e: PurchasesException) {
            emit(CustomerCenterState.Error(e.error))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_FLOW_TIMEOUT),
        initialValue = CustomerCenterState.Loading,
    )

    override suspend fun determineFlow(path: CustomerCenterConfigData.HelpPath) {
        if (path.type == CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE) {
            purchases.awaitRestore()
        }
    }

    private suspend fun loadPurchaseInformation(): PurchaseInformation? {
        val customerInfo = purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)

        // Customer Center WIP: update when we have subscription information in CustomerInfo
        val activeEntitlement = customerInfo.entitlements.active.isEmpty()
        if (activeEntitlement) {
            return CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing
        }

        return null
    }
}
