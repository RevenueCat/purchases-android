package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.GetBillingConfigParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfferingStrings
import java.util.concurrent.atomic.AtomicBoolean

internal class GetBillingConfigUseCase(
    val deviceCache: DeviceCache,
    val onReceive: (BillingConfig) -> Unit,
    val onError: PurchasesErrorCallback,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ((PurchasesError?) -> Unit) -> Unit,
) : BillingClientUseCase<BillingConfig?>(onError, executeRequestOnUIThread) {
    override val errorMessage: String
        get() = "Error getting billing config"

    override fun executeAsync() {
        withConnectedClient {
            val hasResponded = AtomicBoolean(false)

            getBillingConfigAsync(GetBillingConfigParams.newBuilder().build()) { result, config ->
                if (hasResponded.getAndSet(true)) {
                    log(
                        LogIntent.GOOGLE_ERROR,
                        OfferingStrings.EXTRA_GET_BILLING_CONFIG_RESPONSE.format(result.responseCode),
                    )
                    return@getBillingConfigAsync
                }
                processResult(result, config, ::onOk)
            }
        }
    }

    private fun onOk(received: BillingConfig?) {
        if (received == null) {
            onError(PurchasesError(PurchasesErrorCode.StoreProblemError, BillingStrings.BILLING_CONFIG_NULL_ON_SUCCESS))
        } else {
            deviceCache.setStorefront(received.countryCode)
            onReceive(received)
        }
    }
}
