package com.revenuecat.purchasetester

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class OverviewViewModel : ViewModel() {

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo = _customerInfo.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring = _isRestoring.asStateFlow()

    val activeEntitlements = customerInfo.map { info ->
        info?.entitlements?.active?.values?.let {
            formatEntitlements(it)
        } ?: ""
    }

    val allEntitlements = customerInfo.map { info ->
        info?.entitlements?.all?.values?.let {
            formatEntitlements(it)
        } ?: ""
    }

    val verificationResult = customerInfo.map { info ->
        info?.entitlements?.verification ?: VerificationResult.NOT_REQUESTED
    }

    val customerInfoJson = customerInfo.map { info ->
        info?.rawData?.toString(JSON_FORMATTER_INDENT_SPACES) ?: ""
    }

    private val _formattedVirtualCurrencies = MutableStateFlow("")
    val formattedVirtualCurrencies = _formattedVirtualCurrencies.asStateFlow()

    fun onRestoreClicked() {
        _isRestoring.value = true
        Purchases.sharedInstance.restorePurchasesWith(onSuccess = {
            _customerInfo.value = it
            _isRestoring.value = false
        }, onError = {
            _isRestoring.value = false
        })
    }

    fun onBlockStoreClearClicked(context: Context) {
        val blockstoreClient = Blockstore.getClient(context)
        val request = DeleteBytesRequest.Builder()
            .setDeleteAll(true)
            .build()
        blockstoreClient.deleteBytes(request)
            .addOnSuccessListener { Log.d("PurchaseTester", "Blockstore cleared") }
            .addOnFailureListener { Log.e("PurchaseTester", "Blockstore failed to clear: $it") }
    }

    fun retrieveCustomerInfo() {
        viewModelScope.launch {
            try {
                _customerInfo.value = Purchases.sharedInstance.awaitCustomerInfo()
                Log.i("PurchaseTester", "Get Customer info returned Customer info: ${_customerInfo.value}")
            } catch (e: PurchasesException) {
                Log.e("PurchaseTester", "Error getting customer info", e)
            }
        }
    }

    fun onFetchVCsClicked() {
        viewModelScope.launch {
            val virtualCurrencies: VirtualCurrencies = Purchases.sharedInstance.awaitGetVirtualCurrencies()
            val formatted = formatVirtualCurrencies(virtualCurrencies = virtualCurrencies)
            _formattedVirtualCurrencies.value = formatted
            Log.i("PurchaseTester", formatted)
        }
    }

    fun onInvalidateVirtualCurrenciesCache() {
        Purchases.sharedInstance.invalidateVirtualCurrenciesCache()
    }

    fun onFetchVCCache() {
        val cachedVirtualCurrencies: VirtualCurrencies? = Purchases.sharedInstance.cachedVirtualCurrencies
        if (cachedVirtualCurrencies == null) {
            _formattedVirtualCurrencies.value = "Cached VCs are null"
            Log.i("PurchaseTester", "Cached VCs are null")
        } else {
            val formatted = formatVirtualCurrencies(virtualCurrencies = cachedVirtualCurrencies)
            _formattedVirtualCurrencies.value = formatted
            Log.i("PurchaseTester", formatted)
        }
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }

    private fun formatVirtualCurrencies(virtualCurrencies: VirtualCurrencies): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Virtual Currencies (${virtualCurrencies.all.size}):\n")

        if (virtualCurrencies.all.isEmpty()) {
            stringBuilder.append("\tNo virtual currencies available\n")
        } else {
            virtualCurrencies.all.forEach { keyValuePair ->
                stringBuilder.append("\t${keyValuePair.value.code}:\n")
                stringBuilder.append("\t\tName: ${keyValuePair.value.name}\n")
                stringBuilder.append("\t\tBalance: ${keyValuePair.value.balance}\n")
                stringBuilder.append("\t\tDescription: ${keyValuePair.value.serverDescription}\n")
            }
        }

        return stringBuilder.toString()
    }
}
