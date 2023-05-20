package com.revenuecat.purchasetester

import android.net.Uri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoSuspend
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.utils.Result
import kotlinx.coroutines.launch

class OverviewViewModel(private val interactionHandler: OverviewInteractionHandler) : ViewModel() {

    val customerInfo: MutableLiveData<CustomerInfo?> by lazy {
        MutableLiveData<CustomerInfo?>().apply {
            value = null
        }
    }

    val isRestoring = MutableLiveData<Boolean>(false)

    val activeEntitlements = MediatorLiveData<String>()

    val allEntitlements = MediatorLiveData<String>()

    val customerInfoJson = MediatorLiveData<String>()

    // Trusted entitlements: Commented out until ready to be made public
    // val verificationResult = MediatorLiveData<VerificationResult>()

    init {
        activeEntitlements.addSource(customerInfo) { info ->
            info?.entitlements?.active?.values?.let {
                activeEntitlements.value = formatEntitlements(it)
            }
        }

        allEntitlements.addSource(customerInfo) { info ->
            info?.entitlements?.all?.values?.let {
                allEntitlements.value = formatEntitlements(it)
            }
        }

        // Trusted entitlements: Commented out until ready to be made public
//        verificationResult.addSource(customerInfo) { info ->
//            info?.entitlements?.verification?.let {
//                verificationResult.value = it
//            }
//        }

        customerInfoJson.addSource(customerInfo) { info ->
            customerInfoJson.value = info?.rawData?.toString(JSON_FORMATTER_INDENT_SPACES)
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

    fun retrieveCustomerInfo() {
        viewModelScope.launch {
            when (val result = Purchases.sharedInstance.getCustomerInfoSuspend()) {
                is Result.Success -> interactionHandler.customerInfo(result.value)
                is Result.Error -> interactionHandler.customerInfoError(result.value)
            }
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

    fun customerInfo(customerInfo: CustomerInfo)
    fun customerInfoError(error: PurchasesError)
}
