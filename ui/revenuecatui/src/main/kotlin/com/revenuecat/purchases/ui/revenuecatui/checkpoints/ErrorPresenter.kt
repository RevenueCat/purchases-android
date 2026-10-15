package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import dev.drewhamilton.poko.Poko

/**
 * Presents the errors the SDK runs into while serving a checkpoint. Set per call through
 * [CheckpointParams.errorPresenter], which takes precedence, or for every call through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.errorPresenter]. When none is set, the SDK presents its own
 * error dialog through a presenter of its own.
 *
 * An app presenting its own paywall through a [PaywallPresenter] owns that paywall's errors. What happens around
 * it is the SDK's, such as syncing the purchases made in it once it ends, and its errors reach this presenter.
 */
@InviteOnlyCheckpointsAPI
public fun interface ErrorPresenter {

    /**
     * Called on the main thread with an error of a checkpoint. Whatever the SDK has on screen stays there,
     * interactive, underneath whatever the app shows, until [completion] says the app's error UI is done. Present
     * [params]'s error however the app wants, or complete right away to handle it without UI. What the SDK does
     * once the app completes depends on [Params.kind].
     *
     * Errors come one at a time per [com.revenuecat.purchases.Purchases] instance: this is not called again,
     * whichever checkpoint call or kind of failure the next error belongs to, until [completion] has been
     * completed, and then on a later main thread message rather than from inside [Completion.complete]. A
     * presenter may therefore keep a single pending request.
     */
    public fun present(params: Params, completion: Completion)

    /** What an [ErrorPresenter] is asked to present. */
    @InviteOnlyCheckpointsAPI
    @Poko
    public class Params internal constructor(
        /** The error that occurred. */
        public val error: PurchasesError,
        /** The identifier of the checkpoint being served. */
        public val checkpointIdentifier: String,
        /** The custom variables supplied to the checkpoint call. */
        public val customVariables: Map<String, CustomVariableValue>,
        /** What kind of failure the error is, which decides what the SDK does once the app completes. */
        public val kind: CheckpointFailureKind,
    )

    /**
     * How an [ErrorPresenter] tells the SDK that its error UI is done. Only the first call counts; later calls,
     * including calls for a flow that has since ended, are ignored.
     */
    @InviteOnlyCheckpointsAPI
    public fun interface Completion {

        /**
         * Tells the SDK that the app's error UI is done. What happens next depends on [Params.kind]: when the flow
         * could not be shown or an action in it failed, the user stays where they were, back in the app or on the
         * flow still on screen; when its result could not be synced, the checkpoint is passed and its callback
         * receives an empty result.
         */
        public fun complete()
    }
}

/** This presenter as the paywall it serves sees it: the checkpoint's context is added to every error. */
internal fun ErrorPresenter.forCheckpoint(
    checkpointIdentifier: String,
    customVariables: Map<String, CustomVariableValue>,
): PaywallErrorPresenter = PaywallErrorPresenter { error, kind, completion ->
    present(ErrorPresenter.Params(error, checkpointIdentifier, customVariables, kind), completion)
}
