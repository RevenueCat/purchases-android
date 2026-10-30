package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Provides the content of the error dialog the SDK shows when a checkpoint fails. The SDK calls [createDialog]
 * from the main thread with the error information; the factory calls [Completion.complete] with an [Outcome]:
 * custom dialog content, the SDK's own localized copy, or no dialog at all.
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
     * Called on the main thread for each checkpoint error. Call [completion] with the [Outcome] to apply. The
     * completion may be called from any thread, but must be called: the SDK waits for it.
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

    /** What an [ErrorDialogFactory] decides to do with an error. */
    @InternalRevenueCatAPI
    public sealed interface Outcome {

        /** Show the SDK's error dialog with the content of [dialog]. */
        @Poko
        public class Show(public val dialog: CheckpointErrorDialog) : Outcome

        /** Show the SDK's error dialog with its own localized copy. */
        public object UseDefault : Outcome

        /**
         * Show no dialog. The SDK carries on as if the user had acknowledged one, which depends on the kind of
         * failure, exactly as when an [ErrorPresenter] completes without presenting anything.
         */
        public object Suppress : Outcome
    }

    /**
     * How an [ErrorDialogFactory] tells the SDK what to do with an error. Only the first call to [complete] counts;
     * later calls are silently ignored.
     */
    @InternalRevenueCatAPI
    public fun interface Completion {

        /** Applies [outcome]. May be called from any thread; only the first call counts. */
        public fun complete(outcome: Outcome)
    }
}

/**
 * Converts this [ErrorDialogFactory] into an [ErrorPresenter] using [presenterFor] to create the underlying
 * presenter once the factory has responded. When the factory suppresses the error, no presenter is created and the
 * error completes right away.
 *
 * [presenterFor] receives the factory-supplied [CheckpointErrorDialog] when the factory provided one, or `null`
 * when the factory asked for the default dialog or threw.
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
    val response = CompletableDeferred<ErrorDialogFactory.Outcome>()

    fun present(dialog: CheckpointErrorDialog?) {
        presenterFor(dialog).presentOnce(params, outerCompletion::complete)
    }

    val factoryCompletion = ErrorDialogFactory.Completion { outcome -> response.complete(outcome) }

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
        when (val outcome = response.await()) {
            is ErrorDialogFactory.Outcome.Show -> present(outcome.dialog)
            ErrorDialogFactory.Outcome.UseDefault -> present(null)
            ErrorDialogFactory.Outcome.Suppress -> {
                Logger.d("Checkpoint error dialog factory suppressed the error dialog.")
                outerCompletion.complete()
            }
        }
    }
}
