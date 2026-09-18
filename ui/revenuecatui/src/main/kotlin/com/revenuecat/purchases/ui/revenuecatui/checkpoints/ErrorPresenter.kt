package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import dev.drewhamilton.poko.Poko

/**
 * Presents the errors of the flows the SDK presents for checkpoints with app-owned UI. Set per call through
 * [CheckpointParams.errorPresenter], which takes precedence, or for every call through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.errorPresenter]. When none is set, the SDK presents its own
 * error dialog through a presenter of its own.
 *
 * Only errors inside a flow the SDK presents reach it: a workflow, or an offering's paywall when no
 * [PaywallPresenter] is set. An app presenting its own paywall owns that paywall's errors. A checkpoint that could
 * not present anything is not an error of a flow either: it is logged and its callback receives null.
 */
@InternalRevenueCatAPI
public fun interface ErrorPresenter {

    /**
     * Called on the main thread when an error occurs in a flow the SDK presents for a checkpoint. The flow stays
     * on screen, and interactive, underneath whatever the app shows, and waits for [completion] to say how to go
     * on. Present [params]'s error however the app wants, or report right away to handle it without UI.
     */
    public fun present(params: Params, completion: Completion)

    /** What an [ErrorPresenter] is asked to present. */
    @InternalRevenueCatAPI
    @Poko
    public class Params internal constructor(
        /** The error that occurred. */
        public val error: PurchasesError,
        /** The identifier of the checkpoint being served. */
        public val checkpointIdentifier: String,
        /** The custom variables supplied to the checkpoint call. */
        public val customVariables: Map<String, CustomVariableValue>,
        /** What failed. */
        public val source: Source,
        /**
         * Whether the flow can go on after this error. True for a failed purchase or restore: the paywall stays
         * on screen for another try. False when the flow could not be presented or kept on screen: there is
         * nothing to go back to, so [Completion.Result.Retry] ends the flow like [Completion.Result.Continued].
         */
        public val flowCanContinue: Boolean,
    )

    /** What failed in the presented flow. */
    @InternalRevenueCatAPI
    @Poko
    public class Source internal constructor(
        public val name: String,
    ) {

        override fun toString(): String = name

        public companion object {
            /** A purchase started from the flow failed. The flow goes on. */
            @JvmField
            public val PURCHASE: Source = Source("PURCHASE")

            /** A restore started from the flow failed. The flow goes on. */
            @JvmField
            public val RESTORE: Source = Source("RESTORE")

            /** The flow could not be presented or kept on screen (e.g. it has nothing to sell). The flow ends. */
            @JvmField
            public val PRESENTATION: Source = Source("PRESENTATION")
        }
    }

    /**
     * How an [ErrorPresenter] reports the way the flow should go on. Only the first report counts; later reports,
     * including reports for a flow that has since ended, are ignored.
     */
    @InternalRevenueCatAPI
    public fun interface Completion {

        /** Reports how the flow should go on. */
        public fun complete(result: Result)

        /** How the flow goes on after the app's error UI. */
        @InternalRevenueCatAPI
        public abstract class Result internal constructor() {

            /**
             * The error is acknowledged and the flow resumes where it was, so the user can try again. When
             * [Params.flowCanContinue] is false there is nothing to resume, and this ends the flow like [Continued].
             */
            public object Retry : Result() {
                override fun toString(): String = "Retry"
            }

            /**
             * The user goes on: the flow ends as if closed, the user passes the checkpoint, and its callback is
             * invoked once the checkpoint resolves.
             */
            public object Continued : Result() {
                override fun toString(): String = "Continued"
            }

            /**
             * The user backs out: the flow ends as if the user had navigated back, the checkpoint resolves as
             * dismissed, its callback is not invoked, and the app keeps the user where they were.
             */
            public object NavigatedBack : Result() {
                override fun toString(): String = "NavigatedBack"
            }
        }
    }
}

/** This presenter as the paywall it serves sees it: the checkpoint's context is added to every error. */
internal fun ErrorPresenter.forCheckpoint(
    checkpointIdentifier: String,
    customVariables: Map<String, CustomVariableValue>,
): PaywallErrorPresenter = PaywallErrorPresenter { error, source, flowCanContinue, completion ->
    present(ErrorPresenter.Params(error, checkpointIdentifier, customVariables, source, flowCanContinue), completion)
}
