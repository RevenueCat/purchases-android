package com.revenuecat.purchases.models

import android.os.Parcel
import android.os.Parcelable
import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.ProrationMode
import com.revenuecat.purchases.ReplacementMode

/**
 * Enum of possible replacement modes to be passed to a Google Play purchase.
 * Ignored for Amazon purchases.
 *
 * See https://developer.android.com/google/play/billing/subscriptions#proration for examples
 */
enum class GoogleReplacementMode(
    @BillingFlowParams.SubscriptionUpdateParams.ReplacementMode val playBillingClientMode: Int
) : ReplacementMode {
    /**
     * Old subscription is cancelled, and new subscription takes effect immediately.
     * User is charged for the full price of the new subscription on the old subscription's expiration date.
     *
     * This is the default behavior.
     */
    WITHOUT_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION),

    /**
     * Old subscription is cancelled, and new subscription takes effect immediately.
     * Any time remaining on the old subscription is used to push out the first payment date for the new subscription.
     * User is charged the full price of new subscription once that prorated time has passed.
     *
     * The purchase will fail if this mode is used when switching between [SubscriptionOption]s
     * of the same [StoreProduct].
     */
    WITH_TIME_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION),

    /**
     * Replacement takes effect immediately, and the user is charged full price of new plan and is
     * given a full billing cycle of subscription, plus remaining prorated time from the old plan.
     *
     * Example: Samwise's Tier 1 subscription is immediately ended. His Tier 2 subscription begins today and he is
     * charged $36. Since he paid for a full month but used only half of it, half of a month's subscription ($1)
     * is applied to his new subscription. Since that new subscription costs $36/year, he would get 1/36th of a year
     * added on to his subscription period (~10 days). Therefore, Samwise's next charge would be 1 year and 10 days
     * from today for $36. After that, he is charged $36 each year following.
     */
    CHARGE_FULL_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE),

    /**
     * Replacement takes effect immediately, and the billing cycle remains the same.
     *
     * Example: This mode can be used because the Tier 2 subscription price per time unit ($36/year = $3/month) is
     * greater than Tier 1 subscription price per time unit ($2/month). Samwise's Tier 1 subscription is immediately
     * ended.
     * Since he paid for a full month but used only half of it, half of a month's subscription ($1) is applied to
     * his new subscription. However, since that new subscription costs $36/year, the remaining 15 days costs $1.50,
     * so he is charged the difference of $0.50 for his new subscription.
     * On May 1st, Samwise is charged $36 for his new subscription tier and another $36 on May 1 of each year following.
     */
    CHARGE_PRORATED_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE);

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(this.name)
    }

    companion object CREATOR : Parcelable.Creator<GoogleReplacementMode?> {
        fun fromPlayBillingClientMode(
            @BillingFlowParams.SubscriptionUpdateParams.ReplacementMode playBillingClientMode: Int?
        ): GoogleReplacementMode? {
            return playBillingClientMode?.let {
                values().first { playBillingClientMode == it.playBillingClientMode }
            }
        }

        override fun createFromParcel(`in`: Parcel): GoogleReplacementMode? {
            return `in`.readString()?.let { GoogleReplacementMode.valueOf(it) }
        }

        override fun newArray(size: Int): Array<GoogleReplacementMode?> {
            return arrayOfNulls(size)
        }
    }
}
