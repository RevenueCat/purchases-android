package com.revenuecat.purchases.models

import com.revenuecat.purchases.ReplacementMode
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Type of possible replacement modes to be used when performing a subscription product change.
 */
@Parcelize
@Poko
public class StoreReplacementMode internal constructor(
    override val name: String,
) : ReplacementMode {

    override fun toString(): String { return name }

    public companion object {
        /**
         * Old subscription is cancelled, and new subscription takes effect immediately.
         * User is charged for the full price of the new subscription on the old subscription's expiration date.
         *
         * This is the default behavior for the Galaxy and Play stores.
         */
        @JvmField public val WITHOUT_PRORATION: StoreReplacementMode = StoreReplacementMode(
            name = "WITHOUT_PRORATION",
        )

        /**
         * Old subscription is cancelled, and new subscription takes effect immediately.
         * Any time remaining on the old subscription is used to push out the first payment date for
         * the new subscription.
         * User is charged the full price of new subscription once that prorated time has passed.
         *
         * For the Play Store, the purchase will fail if this mode is used when switching between [SubscriptionOption]s
         * of the same [StoreProduct].
         */
        @JvmField public val WITH_TIME_PRORATION: StoreReplacementMode = StoreReplacementMode(
            name = "WITH_TIME_PRORATION",
        )

        /**
         * Replacement takes effect immediately, and the user is charged full price of new plan and is
         * given a full billing cycle of subscription, plus remaining prorated time from the old plan.
         *
         * This mode is not supported by the Galaxy Store. If it is passed to the Galaxy Store, an error will be thrown.
         */
        @JvmField public val CHARGE_FULL_PRICE: StoreReplacementMode = StoreReplacementMode(
            name = "CHARGE_FULL_PRICE",
        )

        /**
         * Replacement takes effect immediately, and the billing cycle remains the same.
         */
        @JvmField public val CHARGE_PRORATED_PRICE: StoreReplacementMode = StoreReplacementMode(
            name = "CHARGE_PRORATED_PRICE",
        )

        /**
         * Replacement takes effect when the old plan expires, and the new price will be charged at the same time.
         */
        @JvmField public val DEFERRED: StoreReplacementMode = StoreReplacementMode(name = "DEFERRED")

        private val allModes: List<StoreReplacementMode> = listOf(
            WITHOUT_PRORATION,
            WITH_TIME_PRORATION,
            CHARGE_FULL_PRICE,
            CHARGE_PRORATED_PRICE,
            DEFERRED,
        )

        internal fun fromName(name: String): StoreReplacementMode? {
            return allModes.firstOrNull { it.name == name }
        }
    }
}
