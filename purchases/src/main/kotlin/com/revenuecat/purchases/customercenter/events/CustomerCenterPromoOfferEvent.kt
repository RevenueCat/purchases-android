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
public class CustomerCenterPromoOfferEvent(
    public val creationData: CreationData = CreationData(),
    public val data: Data,
) : FeatureEvent {

    /**
     * Metadata about when and where this event was created.
     *
     * @property id Unique identifier for this event instance.
     * @property date The date/time when this event was created.
     */
    @Poko
    public class CreationData(
        public val id: UUID = UUID.randomUUID(),
        public val date: Date = Date(),
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
    public class Data(
        public val type: CustomerCenterEventType,
        public val timestamp: Date,
        public val darkMode: Boolean,
        public val locale: String,
        public val version: Int = 1,
        public val revisionID: Int = 1,
        public val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        public val path: CustomerCenterConfigData.HelpPath.PathType,
        public val url: String?,
        public val surveyOptionID: String?,
        public val source: PromoOfferRejectionSource? = null,
        public val storeOfferID: String,
        public val productID: String,
        public val targetProductID: String,
        public val error: String? = null,
        public val transactionID: String? = null,
    )
}
