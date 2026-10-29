package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLayoutDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Provides the content of the error dialog the SDK shows when a checkpoint fails. The SDK calls [createDialog]
 * from the main thread with the error information; the factory calls [Completion.complete] with custom dialog
 * content, or [null] to let the SDK use its own localized copy.
 *
 * The factory may hold the completion and call it later from any thread to let a hybrid bridge wait for the host
 * side to respond. The SDK applies a [TIMEOUT_MS]-millisecond timeout: if the completion is not called in time,
 * the SDK proceeds with its own copy and ignores any later completion.
 *
 * Set per call through [CheckpointParams.Builder.setErrorDialogFactory]. Takes effect when no [ErrorPresenter] is
 * set for the call or registered globally, and with lower precedence than either.
 *
 * @see CheckpointParams
 */
@InternalRevenueCatAPI
public fun interface ErrorDialogFactory {

    /**
     * Called on the main thread for each checkpoint error. Call [completion] with dialog content to show, or
     * [null] to use the SDK's own localized copy. The completion may be called from any thread; the SDK applies a
     * [TIMEOUT_MS]-ms timeout and silently ignores completions that arrive after it.
     *
     * A factory that throws is logged and the SDK falls back to its own dialog; that fallback's completion is
     * the one that counts.
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
     * later calls, including calls made after the SDK's timeout, are silently ignored.
     */
    @InternalRevenueCatAPI
    public fun interface Completion {

        /**
         * Provides dialog content to the SDK, or [null] to use the SDK's own localized copy. May be called
         * from any thread; only the first call counts.
         */
        public fun complete(dialog: CheckpointErrorDialog?)
    }

    public companion object {

        /** The window the SDK waits for the factory before falling back to its own dialog content, in milliseconds. */
        public const val TIMEOUT_MS: Long = 5_000L
    }
}

/**
 * Converts this [ErrorDialogFactory] into an [ErrorPresenter] using [presenterFor] to create the underlying
 * presenter once the factory has responded or the timeout has elapsed.
 *
 * [presenterFor] receives the factory-supplied [CheckpointErrorDialog] when the factory provided one, or [null]
 * when the factory returned null, threw, or timed out (meaning: use the default dialog). This overload is the
 * testable core of the adapter; [CheckpointsManager] passes a production lambda; tests pass a recording one.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
internal fun ErrorDialogFactory.asErrorPresenter(
    scope: CoroutineScope,
    timeoutMs: Long = ErrorDialogFactory.TIMEOUT_MS,
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

    val timeoutJob = scope.launch {
        delay(timeoutMs)
        if (response.complete(null)) {
            Logger.w(
                "Checkpoint error dialog factory timed out after ${timeoutMs}ms; " +
                    "using the SDK's default dialog.",
            )
        }
    }

    val factoryCompletion = ErrorDialogFactory.Completion { dialog ->
        response.complete(dialog)
        // Late completions (after the timeout or a prior completion) are silently ignored.
    }

    try {
        createDialog(factoryParams, factoryCompletion)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        timeoutJob.cancel()
        response.cancel()
        Logger.w("Checkpoint error dialog factory threw; using the SDK's default dialog: $e")
        present(null)
        return@ErrorPresenter
    }

    // Wait until createDialog returns successfully before showing even a synchronous response. If it throws
    // after completing, only the fallback above may present UI.
    scope.launch {
        val dialog = response.await()
        timeoutJob.cancel()
        present(dialog)
    }
}

/**
 * Creates the dialog View for a factory-supplied [CheckpointErrorDialog], using the SDK's existing dialog layout
 * but with the factory-provided [CheckpointErrorDialog.message] and [CheckpointErrorDialog.buttonTitle] in place
 * of the localized defaults. Used by [CheckpointsManager] when wiring a [ErrorDialogFactory] to a
 * [DefaultErrorPresenter].
 */
internal fun factoryErrorDialogContent(
    activity: Activity,
    resources: Context,
    dialog: CheckpointErrorDialog,
    onAcknowledged: () -> Unit,
): View = ComposeView(activity).apply {
    setContent {
        val configuration = resources.resources.configuration
        CompositionLocalProvider(
            LocalContext provides resources,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides configuration.toLayoutDirection(),
        ) {
            ErrorDialog(
                dismissRequest = onAcknowledged,
                error = dialog.message,
                confirmButtonText = dialog.buttonTitle,
            )
        }
    }
}
