package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.BillingResponse
import com.revenuecat.purchases.strings.BillingStrings
import java.io.PrintWriter
import java.io.StringWriter

private const val MAX_RETRIES_DEFAULT = 3

internal abstract class UseCase<T>(
    private val onError: PurchasesErrorCallback,
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

    protected fun BillingClient?.withConnectedClient(receivingFunction: BillingClient.() -> Unit) {
        this?.takeIf { it.isReady }?.let {
            it.receivingFunction()
        } ?: log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_CLIENT_DISCONNECTED.format(getStackTrace()))
    }

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    private fun getStackTrace(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        Throwable().printStackTrace(printWriter)
        return stringWriter.toString()
    }

    abstract fun executeAsync()
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
