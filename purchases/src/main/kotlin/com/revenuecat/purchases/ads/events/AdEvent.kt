@file:OptIn(InternalRevenueCatAPI::class)
@file:Suppress("LongParameterList")

package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.FeatureEvent
import java.util.UUID

internal enum class AdEventType(val value: String) {
    DISPLAYED("rc_ads_ad_displayed"),
    OPENED("rc_ads_ad_opened"),
    REVENUE("rc_ads_ad_revenue"),
}

internal sealed interface AdEvent : FeatureEvent {
    val id: String
    val eventVersion: Int
    val type: AdEventType
    val timestamp: Long
    val networkName: String
    val mediatorName: AdMediatorName
    val placement: String?
    val adUnitId: String
    val impressionId: String

    class Displayed(
        override val id: String = UUID.randomUUID().toString(),
        override val eventVersion: Int = BackendEvent.AD_EVENT_SCHEMA_VERSION,
        override val type: AdEventType = AdEventType.DISPLAYED,
        override val timestamp: Long = System.currentTimeMillis(),
        override val networkName: String,
        override val mediatorName: AdMediatorName,
        override val placement: String?,
        override val adUnitId: String,
        override val impressionId: String,
    ) : AdEvent

    class Open(
        override val id: String = UUID.randomUUID().toString(),
        override val eventVersion: Int = BackendEvent.AD_EVENT_SCHEMA_VERSION,
        override val type: AdEventType = AdEventType.OPENED,
        override val timestamp: Long = System.currentTimeMillis(),
        override val networkName: String,
        override val mediatorName: AdMediatorName,
        override val placement: String?,
        override val adUnitId: String,
        override val impressionId: String,
    ) : AdEvent

    class Revenue(
        override val id: String = UUID.randomUUID().toString(),
        override val eventVersion: Int = BackendEvent.AD_EVENT_SCHEMA_VERSION,
        override val type: AdEventType = AdEventType.REVENUE,
        override val timestamp: Long = System.currentTimeMillis(),
        override val networkName: String,
        override val mediatorName: AdMediatorName,
        override val placement: String?,
        override val adUnitId: String,
        override val impressionId: String,
        val revenueMicros: Long,
        val currency: String,
        val precision: AdRevenuePrecision,
    ) : AdEvent
}
