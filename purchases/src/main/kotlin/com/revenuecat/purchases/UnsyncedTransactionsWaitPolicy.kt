package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

/**
 * Determines whether [Purchases.getCustomerInfo] waits for unsynced purchases to be posted to
 * RevenueCat before reporting [CustomerInfo].
 *
 * When the SDK finds purchases that haven't been synced yet, it posts them and reports the resulting
 * [CustomerInfo]. If posting is slow, every pending `getCustomerInfo` call waits for it, which can
 * delay app launch when `getCustomerInfo` gates the first screen.
 */
public abstract class UnsyncedTransactionsWaitPolicy private constructor(private val policyName: String) {

    override fun toString(): String = "UnsyncedTransactionsWaitPolicy.$policyName"

    public companion object {
        /**
         * Default behavior: [CustomerInfo] is reported once unsynced purchases have been posted.
         */
        @JvmField
        public val WAIT: UnsyncedTransactionsWaitPolicy = Policy("WAIT")

        /**
         * [CustomerInfo] is never held back by unsynced purchases: those are posted in the background,
         * and the up to date [CustomerInfo] is delivered through [UpdatedCustomerInfoListener] once
         * posting finishes.
         *
         * While the posts are in flight, [CustomerInfo] is fetched from RevenueCat as usual. On a first
         * launch, where there's nothing cached to fall back on, it is computed from the purchases on the
         * device instead, so entitlements from those purchases are reported even though RevenueCat
         * doesn't know about them yet.
         *
         * Only set this if your app reacts to [UpdatedCustomerInfoListener]. That's where the
         * [CustomerInfo] that accounts for the posted purchases is delivered, so an app that only reads
         * what its own [Purchases.getCustomerInfo] calls return can keep showing the state from before
         * them.
         *
         * Note that a [CustomerInfo] fetched while a purchase is being posted can report the state
         * before that purchase, and that one computed on the device is not verified by RevenueCat's
         * servers and doesn't include purchases made outside of the store (web purchases, for example).
         *
         * This is best effort: with nothing cached and no way to compute on the device, [CustomerInfo]
         * waits for the purchases to be posted, same as with [WAIT].
         */
        @JvmField
        public val DO_NOT_WAIT: UnsyncedTransactionsWaitPolicy = Policy("DO_NOT_WAIT")
    }

    private class Policy(policyName: String) : UnsyncedTransactionsWaitPolicy(policyName)
}
