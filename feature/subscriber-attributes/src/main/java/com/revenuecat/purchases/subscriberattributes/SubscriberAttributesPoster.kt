package com.revenuecat.purchases.subscriberattributes

import android.net.Uri
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.HTTP_NOT_FOUND_ERROR_CODE
import com.revenuecat.purchases.common.HTTP_SERVER_ERROR_CODE
import com.revenuecat.purchases.common.SubscriberAttributeError

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
            "/subscribers/" + Uri.encode(appUserID) + "/attributes",
            mapOf("attributes" to attributes),
            { error ->
                onErrorHandler(error, false, emptyList())
            },
            { error, responseCode, body ->
                error?.let {
                    val internalServerError = responseCode >= HTTP_SERVER_ERROR_CODE
                    val notFoundError = responseCode == HTTP_NOT_FOUND_ERROR_CODE
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
