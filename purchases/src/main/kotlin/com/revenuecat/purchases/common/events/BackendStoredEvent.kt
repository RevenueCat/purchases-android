package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.AdEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterImpressionEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterSurveyOptionChosenEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stored backend events to be flushed together by EventsManager.
 */
@Serializable
internal sealed class BackendStoredEvent : Event {

    /**
     * Represents a stored event related to the Customer Center.
     *
     * @property event The `BackendEvent.CustomerCenter` event that is being stored.
     */
    @Serializable
    @SerialName("customer_center")
    data class CustomerCenter(val event: BackendEvent.CustomerCenter) : BackendStoredEvent()

    /**
     * Represents a stored event related to Paywalls.
     *
     * @property event The `BackendEvent.Paywalls` event that is being stored.
     */
    @Serializable
    @SerialName("paywalls")
    data class Paywalls(val event: BackendEvent.Paywalls) : BackendStoredEvent()

    /**
     * Represents a stored event related to Ads.
     *
     * @property event The `BackendEvent.Ad` event that is being stored.
     */
    @Serializable
    @SerialName("ad")
    data class Ad(val event: BackendEvent.Ad) : BackendStoredEvent()
}

/**
 * Converts a `BackendStoredEvent` into a `BackendEvent`.
 *
 * @receiver The stored backend event to be converted.
 * @return A `BackendEvent` instance.
 */
internal fun BackendStoredEvent.toBackendEvent(): BackendEvent {
    return when (this) {
        is BackendStoredEvent.Paywalls -> { this.event }
        is BackendStoredEvent.CustomerCenter -> { this.event }
        is BackendStoredEvent.Ad -> { this.event }
    }
}

/**
 * Converts a `PaywallEvent` into a `BackendStoredEvent.Paywalls` instance.
 *
 * @receiver The `PaywallEvent` to be converted.
 * @param appUserID The user ID associated with the event.
 * @return A `BackendStoredEvent.Paywalls` containing a `BackendEvent.Paywalls`.
 */
@OptIn(InternalRevenueCatAPI::class)
@JvmSynthetic
internal fun PaywallEvent.toBackendStoredEvent(
    appUserID: String,
): BackendStoredEvent {
    return BackendStoredEvent.Paywalls(
        BackendEvent.Paywalls(
            id = creationData.id.toString(),
            version = BackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
            type = type.value,
            appUserID = appUserID,
            sessionID = data.sessionIdentifier.toString(),
            offeringID = data.offeringIdentifier,
            paywallRevision = data.paywallRevision,
            timestamp = creationData.date.time,
            displayMode = data.displayMode,
            darkMode = data.darkMode,
            localeIdentifier = data.localeIdentifier,
        ),
    )
}

/**
 * Converts a `CustomerCenterImpressionEvent` into a `BackendStoredEvent.CustomerCenter` instance.
 *
 * @receiver The `CustomerCenterImpressionEvent` to be converted.
 * @param appUserID The user ID associated with the event.
 * @return A `BackendStoredEvent.CustomerCenter` containing a `BackendEvent.CustomerCenter`.
 */
@OptIn(InternalRevenueCatAPI::class)
@JvmSynthetic
internal fun CustomerCenterImpressionEvent.toBackendStoredEvent(
    appUserID: String,
    appSessionID: String,
): BackendStoredEvent {
    return BackendStoredEvent.CustomerCenter(
        BackendEvent.CustomerCenter(
            id = creationData.id.toString(),
            revisionID = data.revisionID,
            type = data.type,
            appUserID = appUserID,
            appSessionID = appSessionID,
            timestamp = data.timestamp.time,
            darkMode = data.darkMode,
            locale = data.locale,
            displayMode = data.displayMode,
            path = null,
            url = null,
            surveyOptionID = null,
        ),
    )
}

/**
 * Converts a `CustomerCenterSurveyOptionChosenEvent` into a `BackendStoredEvent.CustomerCenter` instance.
 *
 * @receiver The `CustomerCenterSurveyOptionChosenEvent` to be converted.
 * @param appUserID The user ID associated with the event.
 * @return A `BackendStoredEvent.CustomerCenter` containing a `BackendEvent.CustomerCenter`.
 */
@OptIn(InternalRevenueCatAPI::class)
@JvmSynthetic
internal fun CustomerCenterSurveyOptionChosenEvent.toBackendStoredEvent(
    appUserID: String,
    appSessionID: String,
): BackendStoredEvent {
    return BackendStoredEvent.CustomerCenter(
        BackendEvent.CustomerCenter(
            id = creationData.id.toString(),
            revisionID = data.revisionID,
            type = data.type,
            appUserID = appUserID,
            appSessionID = appSessionID,
            timestamp = data.timestamp.time,
            darkMode = data.darkMode,
            locale = data.locale,
            displayMode = data.displayMode,
            path = data.path,
            url = data.url,
            surveyOptionID = data.surveyOptionID,
        ),
    )
}

@JvmSynthetic
internal fun AdEvent.Open.toBackendStoredEvent(
    appUserID: String,
    appSessionID: String,
): BackendStoredEvent {
    return BackendStoredEvent.Ad(
        BackendEvent.Ad(
            id = id,
            version = eventVersion,
            type = type.value,
            timestamp = timestamp,
            networkName = networkName,
            mediatorName = mediatorName,
            placement = placement,
            adUnitId = adUnitId,
            adInstanceId = adInstanceId,
            appUserID = appUserID,
            appSessionID = appSessionID,
        ),
    )
}

@JvmSynthetic
internal fun AdEvent.Displayed.toBackendStoredEvent(
    appUserID: String,
    appSessionID: String,
): BackendStoredEvent {
    return BackendStoredEvent.Ad(
        BackendEvent.Ad(
            id = id,
            version = eventVersion,
            type = type.value,
            timestamp = timestamp,
            networkName = networkName,
            mediatorName = mediatorName,
            placement = placement,
            adUnitId = adUnitId,
            adInstanceId = adInstanceId,
            appUserID = appUserID,
            appSessionID = appSessionID,
        ),
    )
}

@JvmSynthetic
internal fun AdEvent.Revenue.toBackendStoredEvent(
    appUserID: String,
    appSessionID: String,
): BackendStoredEvent {
    return BackendStoredEvent.Ad(
        BackendEvent.Ad(
            id = id,
            version = eventVersion,
            type = type.value,
            timestamp = timestamp,
            networkName = networkName,
            mediatorName = mediatorName,
            placement = placement,
            adUnitId = adUnitId,
            adInstanceId = adInstanceId,
            appUserID = appUserID,
            appSessionID = appSessionID,
            revenueMicros = revenueMicros,
            currency = currency,
            precision = precision,
        ),
    )
}
