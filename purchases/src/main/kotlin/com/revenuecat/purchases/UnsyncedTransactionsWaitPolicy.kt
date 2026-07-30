package com.revenuecat.purchases

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
         * When there's no cached [CustomerInfo] to report, it is computed on the device while unsynced
         * purchases are posted in the background, instead of waiting for those posts. The up to date
         * [CustomerInfo] is delivered through [UpdatedCustomerInfoListener] once posting finishes.
         *
         * This covers app launch, where an empty cache is what makes [Purchases.getCustomerInfo] wait
         * for the posts. Once a [CustomerInfo] is cached, [CacheFetchPolicy.CACHED_OR_FETCHED] and
         * [CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT] report it right away, while a fetch that
         * insists on current data still waits for pending posts.
         *
         * Note that the [CustomerInfo] reported while posting is in flight is computed from the
         * device's purchases, so it is not verified by RevenueCat's servers, and purchases made
         * outside of the store (web purchases, for example) are not included in it.
         *
         * This is best effort: when device side computation isn't possible, [CustomerInfo] waits for
         * the purchases to be posted, same as with [WAIT].
         */
        @JvmField
        public val DO_NOT_WAIT: UnsyncedTransactionsWaitPolicy = Policy("DO_NOT_WAIT")
    }

    private class Policy(policyName: String) : UnsyncedTransactionsWaitPolicy(policyName)
}
