package com.revenuecat.purchases.subscriberattributes

import android.net.Uri
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.SubscriberAttributeError

class SubscriberAttributesBackend(
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
            onSuccessHandler,
            { error, notAnInternalServerError, body ->
                var attributeErrors: List<SubscriberAttributeError> = emptyList()
                body?.takeIf { error.code == PurchasesErrorCode.InvalidSubscriberAttributesError }
                    ?.let { body ->
                        attributeErrors = body.getAttributeErrors()
                    }
                onErrorHandler(error, notAnInternalServerError, attributeErrors)
            }
        )
    }
}
