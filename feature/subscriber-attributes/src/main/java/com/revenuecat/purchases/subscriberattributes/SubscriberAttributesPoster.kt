package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes

class SubscriberAttributesPoster(
    private val backend: Backend
) {

    fun postSubscriberAttributes(
        attributes: Map<String, Map<String, Any?>>,
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (
            PurchasesError,
            didBackendGetAttributes: Boolean,
            attributeErrors: List<SubscriberAttributeError>
        ) -> Unit
    ) {
        backend.performRequest(
            Endpoint.PostAttributes(appUserID),
            mapOf("attributes" to attributes),
            { error ->
                onErrorHandler(error, false, emptyList())
            },
            { error, responseCode, body ->
                error?.let {
                    val internalServerError = responseCode >= RCHTTPStatusCodes.ERROR
                    val notFoundError = responseCode == RCHTTPStatusCodes.NOT_FOUND
                    val successfullySynced = !(internalServerError || notFoundError)
                    var attributeErrors: List<SubscriberAttributeError> = emptyList()
                    if (error.code == PurchasesErrorCode.InvalidSubscriberAttributesError) {
                        attributeErrors = body.getAttributeErrors()
                    }
                    onErrorHandler(error, successfullySynced, attributeErrors)
                } ?: onSuccessHandler()
            }
        )
    }
}
