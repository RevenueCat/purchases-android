package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import java.util.Date
import java.util.UUID

@OptIn(InternalRevenueCatAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallTrackingAPI {

    fun check(purchases: Purchases, paywallEvent: PaywallEvent) {
        purchases.track(paywallEvent)
    }

    fun checkPaywallEvent(eventType: PaywallEventType?, paywallRevision: Int) {
        val paywallEvent = PaywallEvent(
            PaywallEvent.CreationData(
                UUID.randomUUID(),
                Date(),
            ),
            PaywallEvent.Data(
                paywallIdentifier = "paywallId",
                "offeringId",
                paywallRevision,
                UUID.randomUUID(),
                "footer",
                "es_ES",
                true,
            ),
            eventType!!,
        )
        val creationData: PaywallEvent.CreationData = paywallEvent.creationData
        val eventUUID: UUID = creationData.id
        val eventDate: Date = creationData.date
        val data: PaywallEvent.Data = paywallEvent.data
        val offeringId: String = data.offeringIdentifier
        val paywallRevision: Int = data.paywallRevision
        val sessionUUID: UUID = data.sessionIdentifier
        val displayMode: String = data.displayMode
        val locale: String = data.localeIdentifier
        val darkMode: Boolean = data.darkMode
    }

    fun checkEventTypes(eventType: PaywallEventType) {
        when (eventType) {
            PaywallEventType.IMPRESSION,
            PaywallEventType.CANCEL,
            PaywallEventType.CLOSE,
            PaywallEventType.EXIT_OFFER,
            PaywallEventType.PURCHASE_INITIATED,
            PaywallEventType.PURCHASE_ERROR,
            -> {}
        }
    }
}
