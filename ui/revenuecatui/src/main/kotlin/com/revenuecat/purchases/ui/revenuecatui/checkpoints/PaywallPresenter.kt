package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import dev.drewhamilton.poko.Poko

/**
 * Presents the offering a checkpoint resolves to with app-owned UI. Set per call through
 * [CheckpointParams.paywallPresenter], which takes precedence, or for every call through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.paywallPresenter]. When none is set, the offering's
 * configured paywall is presented instead, falling back to the default paywall.
 */
@InternalRevenueCatAPI
public fun interface PaywallPresenter {

    /**
     * Called on the main thread when a checkpoint resolves to an offering. Present [params]'s offering however
     * the app wants and report through [completion] how the presentation ended; the checkpoint stays unresolved
     * until then. The SDK works out what the user obtained by itself, so the app only reports how the user left
     * its UI.
     */
    public fun present(params: Params, completion: Completion)

    /** What a [PaywallPresenter] is asked to present. */
    @InternalRevenueCatAPI
    @Poko
    public class Params internal constructor(
        /** The offering the checkpoint resolved to. */
        public val offering: Offering,
        /** The identifier of the checkpoint being served. */
        public val checkpointIdentifier: String,
        /** The custom variables supplied to the checkpoint call. */
        public val customVariables: Map<String, CustomVariableValue>,
    )

    /**
     * How a [PaywallPresenter] reports the way its presentation ended. Only the first report counts; later
     * reports, including reports for a checkpoint call that no longer exists, are ignored.
     *
     * After [Result.Purchased], [Result.Closed] and [Result.ContinuedWithoutPurchasing] the SDK syncs any store
     * purchase made during the presentation, refreshes the customer's information, and resolves the checkpoint with
     * what the user obtained. Purchases made through the SDK or through the app's own billing client are both
     * picked up; an app that disabled automatic purchase syncing must call
     * [com.revenuecat.purchases.Purchases.syncPurchases] before reporting.
     */
    @InternalRevenueCatAPI
    public fun interface Completion {

        /** Reports how the app's UI ended. */
        public fun complete(result: Result)

        /** The way the user left the app's UI. */
        @InternalRevenueCatAPI
        public abstract class Result internal constructor() {

            /** The user purchased or restored from the app's UI, which is now gone. */
            public object Purchased : Result() {
                override fun toString(): String = "Purchased"
            }

            /**
             * The user left the app's UI through a close action without purchasing. The user passes the
             * checkpoint, so its callback is invoked once the checkpoint resolves.
             */
            public object Closed : Result() {
                override fun toString(): String = "Closed"
            }

            /**
             * The user backed out of the app's UI (e.g. system back) without purchasing. Nothing is synced: the
             * checkpoint resolves as dismissed, its callback is not invoked, and the app keeps the user where
             * they were.
             */
            public object NavigatedBack : Result() {
                override fun toString(): String = "NavigatedBack"
            }

            /**
             * The user chose to continue past the app's UI without purchasing. Behaves like [Closed] today; the
             * two will diverge once flows can carry on after the app's UI.
             */
            public object ContinuedWithoutPurchasing : Result() {
                override fun toString(): String = "ContinuedWithoutPurchasing"
            }
        }
    }
}
