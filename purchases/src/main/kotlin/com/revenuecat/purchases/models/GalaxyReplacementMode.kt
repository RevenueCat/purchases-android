package com.revenuecat.purchases.models

import android.os.Parcel
import android.os.Parcelable
import com.revenuecat.purchases.ReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
/**
 * Enum of possible replacement modes to be passed to a Samsung Galaxy Store subscription change.
 * Ignored for Google Play and Amazon purchases.
 *
 * See https://developer.samsung.com/iap/subscription-guide/manage-subscription-plan/proration-modes.html
 * for more details.
 */
enum class GalaxyReplacementMode(
    val samsungProrationMode: HelperDefine.ProrationMode,
) : ReplacementMode {
    /**
     * The current subscription is instantly changed and the customer can start using the new subscription
     * immediately. The remaining payment of the original subscription is prorated to the cost of the new
     * subscription (based on the daily price). The payment (renewal) date and starting day of the subscription
     * period are changed based on this calculation.
     *
     * This mode can be used for both upgrades and downgrades.
     *
     * This is the default behavior.
     */
    INSTANT_PRORATED_DATE(HelperDefine.ProrationMode.INSTANT_PRORATED_DATE),

    /**
     * For upgraded subscriptions only. The current subscription is instantly changed and the customer can
     * start using the new subscription immediately. While the starting day of the subscription period and
     * payment (renewal) date remain the same, the prorated cost of the upgraded subscription for the remainder
     * of the subscription period (minus the remaining payment of the original subscription) is immediately
     * charged to the customer.
     */
    INSTANT_PRORATED_CHARGE(HelperDefine.ProrationMode.INSTANT_PRORATED_CHARGE),

    /**
     * For upgraded subscriptions only. The current subscription is instantly changed and the customer can
     * start using the new subscription immediately. The new subscription rate is not applied until the current
     * subscription period ends. The payment (renewal) date remains the same. There are no extra charges to use
     * the upgraded subscription during the current subscription period.
     */
    INSTANT_NO_PRORATION(HelperDefine.ProrationMode.INSTANT_NO_PRORATION),

    /**
     * The current subscription continues and the features of the new subscription are not available until
     * the current subscription period ends. The new subscription price is charged and the features of the
     * new subscription are available when the subscription is renewed. When the customer changes their
     * subscription, they cannot change the subscription again during the remaining time of the current
     * subscription period.
     */
    DEFERRED(HelperDefine.ProrationMode.DEFERRED),
    ;

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(this.name)
    }

    companion object CREATOR : Parcelable.Creator<GalaxyReplacementMode?> {
        /**
         * The default replacement mode for Galaxy Store subscription changes.
         */
        val default: GalaxyReplacementMode = INSTANT_PRORATED_DATE

        fun fromSamsungProrationMode(samsungProrationMode: HelperDefine.ProrationMode?): GalaxyReplacementMode? {
            return samsungProrationMode?.let { mode ->
                values().firstOrNull { mode == it.samsungProrationMode }
            }
        }

        override fun createFromParcel(`in`: Parcel): GalaxyReplacementMode? {
            return `in`.readString()?.let { GalaxyReplacementMode.valueOf(it) }
        }

        override fun newArray(size: Int): Array<GalaxyReplacementMode?> {
            return arrayOfNulls(size)
        }
    }
}
