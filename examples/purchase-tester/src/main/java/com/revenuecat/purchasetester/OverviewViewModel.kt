package com.revenuecat.purchasetester

import android.view.View
import android.widget.Toast
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.restorePurchasesWith

class OverviewViewModel(val interactionHandler: OverviewInteractionHandler) : ViewModel() {

    val customerInfo: MutableLiveData<CustomerInfo> by lazy {
        MutableLiveData<CustomerInfo>().apply {
            value = null
        }
    }

    val isRestoring = MutableLiveData<Boolean>(false)

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

    fun onRestoreClicked() {
        isRestoring.value = true
        Purchases.sharedInstance.restorePurchasesWith(onSuccess = {
            customerInfo.postValue(it)
            interactionHandler.showToast("Restoring purchases successful, check for new customer info")
            isRestoring.value = false
        }, onError = {
            interactionHandler.displayError(it)
            isRestoring.value = false
        })
    }
}

interface OverviewInteractionHandler {
    fun displayError(error: PurchasesError)
    fun showToast(message: String)
}