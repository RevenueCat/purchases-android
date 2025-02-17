package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
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
}

/**
 * Converts a `BackendStoredEvent` into a `BackendEvent`.
 *
 * @receiver The stored backend event to be converted.
 * @return A `BackendEvent` instance.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun BackendStoredEvent.toBackendEvent(): BackendEvent {
    return when (this) {
        is BackendStoredEvent.Paywalls -> { this.event }
        is BackendStoredEvent.CustomerCenter -> { this.event }
    }
}

/**
 * Converts a `PaywallEvent` into a `BackendStoredEvent.Paywalls` instance.
 *
 * @receiver The `PaywallEvent` to be converted.
 * @param appUserID The user ID associated with the event.
 * @return A `BackendStoredEvent.Paywalls` containing a `BackendEvent.Paywalls`.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
            surveyOptionTitleKey = null,
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
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
            surveyOptionTitleKey = data.surveyOptionTitleKey,
        ),
    )
}
