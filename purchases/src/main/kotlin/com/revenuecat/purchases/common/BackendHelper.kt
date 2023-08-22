package com.revenuecat.purchases.common

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import org.json.JSONObject

internal class BackendHelper(
    private val apiKey: String,
    private val dispatcher: Dispatcher,
    private val appConfig: AppConfig,
    private val httpClient: HTTPClient,
) {
    internal val authenticationHeaders = mapOf("Authorization" to "Bearer ${this.apiKey}")

    @Suppress("LongParameterList")
    fun performRequest(
        endpoint: Endpoint,
        body: Map<String, Any?>?,
        postFieldsToSign: List<Pair<String, String>>?,
        delay: Delay,
        onError: (PurchasesError) -> Unit,
        onCompleted: (PurchasesError?, Int, JSONObject) -> Unit,
    ) {
        enqueue(
            object : Dispatcher.AsyncCall() {
                override fun call(): HTTPResult {
                    return httpClient.performRequest(
                        appConfig.baseURL,
                        endpoint,
                        body,
                        postFieldsToSign,
                        authenticationHeaders,
                    )
                }

                override fun onError(error: PurchasesError) {
                    onError(error)
                }

                override fun onCompletion(result: HTTPResult) {
                    val error = if (!result.isSuccessful()) {
                        result.toPurchasesError().also { errorLog(it) }
                    } else {
                        null
                    }
                    onCompleted(error, result.responseCode, result.body)
                }
            },
            dispatcher,
            delay,
        )
    }

    fun enqueue(
        call: Dispatcher.AsyncCall,
        dispatcher: Dispatcher,
        delay: Delay = Delay.NONE,
    ) {
        if (dispatcher.isClosed()) {
            errorLog("Enqueuing operation in closed dispatcher.")
        } else {
            dispatcher.enqueue(call, delay)
        }
    }
}

internal fun HTTPResult.isSuccessful(): Boolean {
    return responseCode < RCHTTPStatusCodes.UNSUCCESSFUL
}
