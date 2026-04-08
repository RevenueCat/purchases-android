package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(InternalRevenueCatAPI::class)
@Serializable
internal data class PaywallStoredEvent(
    val event: PaywallEvent,
    val userID: String,
) : Event {
    companion object {
        val json = Json.Default
        fun fromString(string: String): PaywallStoredEvent {
            return json.decodeFromString(string)
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun toBackendEvent(): BackendEvent.Paywalls {
        val backendControlFields = event.controlInteraction.toBackendControlFields()
        return BackendEvent.Paywalls(
            id = event.creationData.id.toString(),
            version = BackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
            type = event.type.value,
            appUserID = userID,
            sessionID = event.data.sessionIdentifier.toString(),
            offeringID = event.data.presentedOfferingContext.offeringIdentifier,
            paywallID = event.data.paywallIdentifier,
            paywallRevision = event.data.paywallRevision,
            timestamp = event.creationData.date.time,
            displayMode = event.data.displayMode,
            darkMode = event.data.darkMode,
            localeIdentifier = event.data.localeIdentifier,
            exitOfferType = event.data.exitOfferType?.value,
            exitOfferingID = event.data.exitOfferingIdentifier,
            packageID = event.data.packageIdentifier,
            productID = event.data.productIdentifier,
            errorCode = event.data.errorCode,
            errorMessage = event.data.errorMessage,
            componentType = backendControlFields.componentType,
            componentName = backendControlFields.componentName,
            componentValue = backendControlFields.componentValue,
            componentUrl = backendControlFields.componentUrl,
            originIndex = backendControlFields.originIndex,
            destinationIndex = backendControlFields.destinationIndex,
            originContextName = backendControlFields.originContextName,
            destinationContextName = backendControlFields.destinationContextName,
            defaultIndex = backendControlFields.defaultIndex,
            originPackageIdentifier = backendControlFields.originPackageIdentifier,
            destinationPackageIdentifier = backendControlFields.destinationPackageIdentifier,
            defaultPackageIdentifier = backendControlFields.defaultPackageIdentifier,
            originProductIdentifier = backendControlFields.originProductIdentifier,
            destinationProductIdentifier = backendControlFields.destinationProductIdentifier,
            defaultProductIdentifier = backendControlFields.defaultProductIdentifier,
            currentPackageIdentifier = backendControlFields.currentPackageIdentifier,
            resultingPackageIdentifier = backendControlFields.resultingPackageIdentifier,
            currentProductIdentifier = backendControlFields.currentProductIdentifier,
            resultingProductIdentifier = backendControlFields.resultingProductIdentifier,
        )
    }

    override fun toString(): String {
        return json.encodeToString(this)
    }
}
