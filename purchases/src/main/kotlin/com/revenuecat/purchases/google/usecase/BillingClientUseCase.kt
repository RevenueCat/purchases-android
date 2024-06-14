package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.min
import com.revenuecat.purchases.google.BillingResponse
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.strings.BillingStrings
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal typealias ExecuteRequestOnUIThreadFunction = (delayInMillis: Long, onError: (PurchasesError?) -> Unit) -> Unit

private const val MAX_RETRIES_DEFAULT = 3
private val RETRY_TIMER_START = 878.milliseconds // So it gets close to 15 minutes in last retry
internal val RETRY_TIMER_MAX_TIME = 15.minutes
internal val RETRY_TIMER_SERVICE_UNAVAILABLE_MAX_TIME_FOREGROUND = 4.seconds

internal interface UseCaseParams {
    val appInBackground: Boolean
}

internal abstract class BillingClientUseCase<T>(
    private val useCaseParams: UseCaseParams,
    private val onError: PurchasesErrorCallback,
    val executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) {

    abstract val errorMessage: String

    protected open val backoffForNetworkErrors = false

    private val maxRetries: Int = MAX_RETRIES_DEFAULT
    private var retryAttempt: Int = 0
    private var retryBackoff = RETRY_TIMER_START

    fun run(
        delayMilliseconds: Long = 0,
    ) {
        executeRequestOnUIThread(delayMilliseconds) { connectionError ->
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
        onSuccess: (T) -> Unit = ::onOk,
        onError: (BillingResult) -> Unit = ::forwardError,
    ) {
        when (BillingResponse.fromCode(billingResult.responseCode)) {
            BillingResponse.OK -> {
                retryBackoff = RETRY_TIMER_START
                onSuccess(response)
            }

            BillingResponse.ServiceDisconnected -> {
                log(LogIntent.GOOGLE_ERROR, BillingStrings.BILLING_SERVICE_DISCONNECTED)
                run()
            }

            BillingResponse.ServiceUnavailable -> {
                backoffOrErrorIfServiceUnavailable(onError, billingResult)
            }

            BillingResponse.NetworkError,
            BillingResponse.Error,
            -> {
                backoffOrRetryNetworkError(onError, billingResult)
            }

            else -> {
                onError(billingResult)
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

    private fun forwardError(billingResult: BillingResult) {
        val underlyingErrorMessage = "$errorMessage - ${billingResult.toHumanReadableDescription()}"
        log(LogIntent.GOOGLE_ERROR, underlyingErrorMessage)
        onError(
            billingResult.responseCode.billingResponseToPurchasesError(
                underlyingErrorMessage,
            ).also { errorLog(it) },
        )
    }

    private fun backoffOrRetryNetworkError(
        onError: (BillingResult) -> Unit,
        billingResult: BillingResult,
    ) {
        if (backoffForNetworkErrors && retryBackoff < RETRY_TIMER_MAX_TIME) {
            retryWithBackoff()
        } else if (!backoffForNetworkErrors && retryAttempt < maxRetries) {
            retryAttempt++
            executeAsync()
        } else {
            onError(billingResult)
        }
    }

    private fun backoffOrErrorIfServiceUnavailable(
        onError: (BillingResult) -> Unit,
        billingResult: BillingResult,
    ) {
        log(LogIntent.GOOGLE_WARNING, BillingStrings.BILLING_SERVICE_UNAVAILABLE.format(useCaseParams.appInBackground))
        val maxBackoff = if (useCaseParams.appInBackground) {
            RETRY_TIMER_MAX_TIME
        } else {
            RETRY_TIMER_SERVICE_UNAVAILABLE_MAX_TIME_FOREGROUND
        }
        if (retryBackoff < maxBackoff) {
            retryWithBackoff()
        } else {
            onError(billingResult)
        }
    }

    private fun retryWithBackoff() {
        retryBackoff.let { currentDelay ->
            retryBackoff = min(
                retryBackoff * 2,
                RETRY_TIMER_MAX_TIME,
            )
            run(currentDelay.inWholeMilliseconds)
        }
    }
}
