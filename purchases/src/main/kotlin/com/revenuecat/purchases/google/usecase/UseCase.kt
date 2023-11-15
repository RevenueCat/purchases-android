package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.BillingResponse
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.strings.BillingStrings
import java.io.PrintWriter
import java.io.StringWriter

private const val MAX_RETRIES_DEFAULT = 3

internal abstract class UseCase<T>(
    private val onError: PurchasesErrorCallback,
    val executeRequestOnUIThread: ((PurchasesError?) -> Unit) -> Unit,
) {

    abstract val errorMessage: String

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

    abstract fun executeAsync()
    abstract fun onOk(received: T)

    fun processResult(
        billingResult: BillingResult,
        response: T,
    ) {
        when (BillingResponse.fromCode(billingResult.responseCode)) {
            BillingResponse.OK -> {
                onOk(response)
            }

            BillingResponse.ServiceDisconnected -> {
                log(LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_SERVICE_DISCONNECTED)
                run()
            }

            BillingResponse.NetworkError,
            BillingResponse.ServiceUnavailable,
            BillingResponse.Error,
            -> {
                if (retryAttempt < maxRetries) {
                    retryAttempt++
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

    private fun forwardError(billingResult: BillingResult, onError: PurchasesErrorCallback) {
        val underlyingErrorMessage = "$errorMessage - ${billingResult.toHumanReadableDescription()}"
        log(LogIntent.GOOGLE_ERROR, underlyingErrorMessage)
        onError(
            billingResult.responseCode.billingResponseToPurchasesError(
                underlyingErrorMessage,
            ).also { errorLog(it) },
        )
    }
}
