package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.google.BillingResponse

private const val MAX_RETRIES_DEFAULT = 3

abstract class UseCase<T>(
    private val onError: PurchasesErrorCallback,
    val executeAsync: UseCase<T>.() -> Unit,
    val executeRequestOnUIThread: ((PurchasesError?) -> Unit) -> Unit,
) {
    private val maxRetries: Int = MAX_RETRIES_DEFAULT
    private var retryAttempt: Int = 0

    fun run() {
        executeRequestOnUIThread { connectionError ->
            if (connectionError == null) {
                this.executeAsync()
            } else {
                onError(connectionError)
            }
        }
    }

    abstract fun onOk(received: T)
    abstract fun forwardError(billingResult: BillingResult, onError: PurchasesErrorCallback)

    fun processResult(
        billingResult: BillingResult,
        response: T,
    ) {
        when (BillingResponse.fromCode(billingResult.responseCode)) {
            BillingResponse.OK -> {
                onOk(response)
            }

            BillingResponse.ServiceDisconnected -> {
                run()
            }

            BillingResponse.NetworkError,
            BillingResponse.ServiceUnavailable,
            BillingResponse.Error,
            -> {
                if (retryAttempt < maxRetries) {
                    executeAsync()
                } else {
                    forwardError(billingResult, onError)
                }
            }

            else -> {
                forwardError(billingResult, onError)
            }
        }
    }
}
