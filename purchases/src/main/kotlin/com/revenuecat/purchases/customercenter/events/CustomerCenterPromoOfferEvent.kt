package com.revenuecat.purchases.customercenter.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import dev.drewhamilton.poko.Poko
import java.util.Date
import java.util.UUID

/**
 * Event representing a promotional offer interaction in the Customer Center.
 *
 * This event tracks the lifecycle of promotional offers including impressions (when shown),
 * dismissals (passive dismissal before purchase), cancellations (during Google purchase flow),
 * successful purchases, rejections, and errors.
 *
 * @property creationData Metadata about when and where the event was created.
 * @property data The specific data payload for this event.
 */
@InternalRevenueCatAPI
@Poko
class CustomerCenterPromoOfferEvent(
    val creationData: CreationData = CreationData(),
    val data: Data,
) : FeatureEvent {

    /**
     * Metadata about when and where this event was created.
     *
     * @property id Unique identifier for this event instance.
     * @property date The date/time when this event was created.
     */
    @Poko
    class CreationData(
        val id: UUID = UUID.randomUUID(),
        val date: Date = Date(),
    )

    /**
     * The data payload for a promo offer event.
     *
     * @property type The type of promo offer event (impression, dismissed, cancel, success, rejected, error).
     * @property timestamp The date/time when the event occurred.
     * @property darkMode Whether the app was in dark mode at the time of the event.
     * @property locale The locale identifier of the device.
     * @property version The version number of the event schema.
     * @property revisionID The revision ID of the Customer Center configuration.
     * @property displayMode The display mode of the Customer Center.
     * @property path The help path type that led to this promo offer.
     * @property url The URL if the path type is CUSTOM_URL.
     * @property surveyOptionID The ID of the survey option selected, if applicable.
     * @property source The rejection source if the event type is PROMO_OFFER_REJECTED.
     * @property storeOfferID The store-specific identifier for the promotional offer.
     * @property productID The product identifier for the promotional offer.
     * @property targetProductID The target product identifier associated with the promotional offer.
     * @property error An error message if the event type is PROMO_OFFER_ERROR.
     * @property transactionID The transaction identifier if the event type is PROMO_OFFER_SUCCESS.
     */
    @InternalRevenueCatAPI
    @Poko
    @SuppressWarnings("LongParameterList")
    class Data(
        val type: CustomerCenterEventType,
        val timestamp: Date,
        val darkMode: Boolean,
        val locale: String,
        val version: Int = 1,
        val revisionID: Int = 1,
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        val path: CustomerCenterConfigData.HelpPath.PathType,
        val url: String?,
        val surveyOptionID: String?,
        val source: PromoOfferRejectionSource? = null,
        val storeOfferID: String,
        val productID: String,
        val targetProductID: String,
        val error: String? = null,
        val transactionID: String? = null,
    )
}
