package com.revenuecat.purchasetester

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo

class OverviewViewModel : ViewModel() {

    val customerInfo: MutableLiveData<CustomerInfo> by lazy {
        MutableLiveData<CustomerInfo>().apply {
            value = null
        }
    }

    val activeEntitlements = MediatorLiveData<String>()

    val allEntitlements = MediatorLiveData<String>()

    init {
        activeEntitlements.addSource(customerInfo) { info ->
            activeEntitlements.value = formatEntitlements(info.entitlements.active.values)
        }

        allEntitlements.addSource(customerInfo) { info ->
            allEntitlements.value = formatEntitlements(info.entitlements.all.values)
        }
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }
}