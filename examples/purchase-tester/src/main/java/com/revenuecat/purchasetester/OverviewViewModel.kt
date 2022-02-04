package com.revenuecat.purchasetester

import android.net.Uri
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

class OverviewViewModel(private val interactionHandler: OverviewInteractionHandler) : ViewModel() {

    val customerInfo: MutableLiveData<CustomerInfo> by lazy {
        MutableLiveData<CustomerInfo>().apply {
            value = null
        }
    }

    val isRestoring = MutableLiveData<Boolean>(false)

    val activeEntitlements = MediatorLiveData<String>()

    val allEntitlements = MediatorLiveData<String>()

    val customerInfoJson = MediatorLiveData<String>()

    init {
        activeEntitlements.addSource(customerInfo) { info ->
            activeEntitlements.value = formatEntitlements(info.entitlements.active.values)
        }

        allEntitlements.addSource(customerInfo) { info ->
            allEntitlements.value = formatEntitlements(info.entitlements.all.values)
        }

        customerInfoJson.addSource(customerInfo) { info ->
            customerInfoJson.value = info.rawData.toString(JSON_FORMATTER_INDENT_SPACES)
        }
    }

    fun onRestoreClicked() {
        isRestoring.value = true
        Purchases.sharedInstance.restorePurchasesWith(onSuccess = {
            customerInfo.postValue(it)
            interactionHandler.showToast("Restoring successful")
            isRestoring.value = false
        }, onError = {
            interactionHandler.displayError(it)
            isRestoring.value = false
        })
    }

    fun onCardClicked() = interactionHandler.toggleCard()

    fun onCopyClicked() {
        customerInfo.value?.originalAppUserId?.let {
            interactionHandler.copyToClipboard(it)
        }
    }

    fun onManageClicked() {
        customerInfo.value?.managementURL?.let {
            interactionHandler.launchURL(it)
        }
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }
}

interface OverviewInteractionHandler {
    fun displayError(error: PurchasesError)
    fun showToast(message: String)
    fun toggleCard()
    fun copyToClipboard(text: String)
    fun launchURL(url: Uri)
}