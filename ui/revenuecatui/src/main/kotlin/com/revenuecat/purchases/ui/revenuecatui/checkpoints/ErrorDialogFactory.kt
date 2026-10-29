package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Provides the content of the error dialog the SDK shows when a checkpoint fails. The SDK calls [createDialog]
 * from the main thread with the error information; the factory calls [Completion.complete] with custom dialog
 * content, or `null` to let the SDK use its own localized copy.
 *
 * The factory may hold the completion and call it later from any thread to let a hybrid bridge wait for the host
 * side to respond. Like [ErrorPresenter.Completion], the completion must be called: the SDK shows nothing further
 * until it is.
 *
 * Set per call through [CheckpointParams.Builder.setErrorDialogFactory]. Used only when no [ErrorPresenter] is
 * set for the call or registered globally.
 *
 * @see CheckpointParams
 */
@InternalRevenueCatAPI
public fun interface ErrorDialogFactory {

    /**
     * Called on the main thread for each checkpoint error. Call [completion] with dialog content to show, or
     * `null` to use the SDK's own localized copy. The completion may be called from any thread, but must be
     * called: the SDK waits for it.
     *
     * A factory that throws is logged and the SDK shows its own dialog; any completion the factory already made
     * is ignored.
     */
    public fun createDialog(params: Params, completion: Completion)

    /** What an [ErrorDialogFactory] receives when asked for dialog content. */
    @InternalRevenueCatAPI
    public class Params internal constructor(
        /** The error that occurred. */
        public val error: PurchasesError,
        /** The identifier of the checkpoint being served. */
        public val checkpointIdentifier: String,
        /** The custom variables supplied to the checkpoint call. */
        public val customVariables: Map<String, CustomVariableValue>,
        /** What kind of failure the error is. */
        @InviteOnlyCheckpointsAPI
        public val kind: CheckpointFailureKind,
    ) {

        override fun equals(other: Any?): Boolean =
            other is Params &&
                other.error == error &&
                other.checkpointIdentifier == checkpointIdentifier &&
                other.customVariables == customVariables &&
                other.kind == kind

        override fun hashCode(): Int {
            var result = error.hashCode()
            result = 31 * result + checkpointIdentifier.hashCode()
            result = 31 * result + customVariables.hashCode()
            result = 31 * result + kind.hashCode()
            return result
        }

        override fun toString(): String =
            "ErrorDialogFactory.Params(error=$error, checkpointIdentifier=$checkpointIdentifier, " +
                "customVariables=$customVariables, kind=$kind)"
    }

    /**
     * How an [ErrorDialogFactory] provides dialog content to the SDK. Only the first call to [complete] counts;
     * later calls are silently ignored.
     */
    @InternalRevenueCatAPI
    public fun interface Completion {

        /**
         * Provides dialog content to the SDK, or `null` to use the SDK's own localized copy. May be called
         * from any thread; only the first call counts.
         */
        public fun complete(dialog: CheckpointErrorDialog?)
    }
}

/**
 * Converts this [ErrorDialogFactory] into an [ErrorPresenter] using [presenterFor] to create the underlying
 * presenter once the factory has responded.
 *
 * [presenterFor] receives the factory-supplied [CheckpointErrorDialog] when the factory provided one, or `null`
 * when the factory returned null or threw (meaning: use the default dialog).
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
internal fun ErrorDialogFactory.asErrorPresenter(
    scope: CoroutineScope,
    presenterFor: (CheckpointErrorDialog?) -> ErrorPresenter,
): ErrorPresenter = ErrorPresenter { params, outerCompletion ->
    val factoryParams = ErrorDialogFactory.Params(
        error = params.error,
        checkpointIdentifier = params.checkpointIdentifier,
        customVariables = params.customVariables,
        kind = params.kind,
    )
    val response = CompletableDeferred<CheckpointErrorDialog?>()

    fun present(dialog: CheckpointErrorDialog?) {
        presenterFor(dialog).presentOnce(params, outerCompletion::complete)
    }

    val factoryCompletion = ErrorDialogFactory.Completion { dialog -> response.complete(dialog) }

    try {
        createDialog(factoryParams, factoryCompletion)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w("Checkpoint error dialog factory threw; using the SDK's default dialog: $e")
        present(null)
        return@ErrorPresenter
    }

    // Wait until createDialog returns successfully before showing even a synchronous response. If it throws
    // after completing, only the fallback above may present UI.
    scope.launch {
        present(response.await())
    }
}
