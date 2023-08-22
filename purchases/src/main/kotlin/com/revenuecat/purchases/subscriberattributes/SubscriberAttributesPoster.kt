package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes

internal class SubscriberAttributesPoster(
    private val backendHelper: BackendHelper,
) {

    fun postSubscriberAttributes(
        attributes: Map<String, Map<String, Any?>>,
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (
            PurchasesError,
            didBackendGetAttributes: Boolean,
            attributeErrors: List<SubscriberAttributeError>,
        ) -> Unit,
    ) {
        backendHelper.performRequest(
            Endpoint.PostAttributes(appUserID),
            mapOf("attributes" to attributes),
            postFieldsToSign = null,
            delay = Delay.DEFAULT,
            { error ->
                onErrorHandler(error, false, emptyList())
            },
            { error, responseCode, body ->
                error?.let {
                    val internalServerError = RCHTTPStatusCodes.isServerError(responseCode)
                    val notFoundError = responseCode == RCHTTPStatusCodes.NOT_FOUND
                    val successfullySynced = !(internalServerError || notFoundError)
                    var attributeErrors: List<SubscriberAttributeError> = emptyList()
                    if (error.code == PurchasesErrorCode.InvalidSubscriberAttributesError) {
                        attributeErrors = body.getAttributeErrors()
                    }
                    onErrorHandler(error, successfullySynced, attributeErrors)
                } ?: onSuccessHandler()
            },
        )
    }
}
