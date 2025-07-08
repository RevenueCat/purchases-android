package com.revenuecat.purchasetester

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
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

    val verificationResult = MediatorLiveData<VerificationResult>()

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

        verificationResult.addSource(customerInfo) { info ->
            info?.entitlements?.verification?.let {
                verificationResult.value = it
            }
        }

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
            try {
                customerInfo.value = Purchases.sharedInstance.awaitCustomerInfo()
                Log.i("PurchaseTester", "Get Customer info returned Customer info: ${customerInfo.value}")
            } catch (e: PurchasesException) {
                interactionHandler.displayError(e.error)
            }
        }
    }

    fun onSetAttributeClicked() {
        interactionHandler.setAttribute()
    }

    fun onSyncAttributesClicked() {
        interactionHandler.syncAttributes()
    }

    fun onFetchVCsClicked() {
        viewModelScope.launch {
            val virtualCurrencies: VirtualCurrencies = Purchases.sharedInstance.awaitGetVirtualCurrencies()
            Log.i("PurchaseTester", formatVirtualCurrencies(virtualCurrencies = virtualCurrencies))
        }
    }

    fun onInvalidateVirtualCurrenciesCache() {
        Purchases.sharedInstance.invalidateVirtualCurrenciesCache()
    }

    fun onFetchVCCache() {
        val cachedVirtualCurrencies: VirtualCurrencies? = Purchases.sharedInstance.cachedVirtualCurrencies
        if (cachedVirtualCurrencies == null) {
            Log.i("PurchaseTester", "Cached VCs are null")
        } else {
            formatVirtualCurrencies(virtualCurrencies = cachedVirtualCurrencies)
                .let { Log.i("PurchaseTester", it) }
        }
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }

    private fun formatVirtualCurrencies(virtualCurrencies: VirtualCurrencies): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("----- Virtual Currencies:\n")
        for (virtualCurrency in virtualCurrencies.all) {
            stringBuilder.append("\t")
            stringBuilder.append(virtualCurrency.toString())
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }
}

interface OverviewInteractionHandler {
    fun displayError(error: PurchasesError)
    fun showToast(message: String)
    fun toggleCard()
    fun copyToClipboard(text: String)
    fun launchURL(url: Uri)
    fun setAttribute()
    fun syncAttributes()
}
