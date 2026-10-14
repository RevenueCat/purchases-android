package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hands checkpoint errors to their [ErrorPresenter] one at a time: the next error, whichever checkpoint call,
 * presenter instance or stage it comes from, is presented only once the previous completion has completed. The
 * SDK waits on those completions, and a checkpoint keeps running while an earlier one is still syncing or showing
 * an error, so without this an app presenter could be asked to present again before it was done, and one that
 * keeps a single pending request would drop the first and leave its checkpoint waiting forever.
 *
 * Main-thread only, like the [CheckpointsManager] that owns it; completions may arrive on any thread and are
 * brought back to [scope] before the queue moves on.
 */
internal class ErrorPresentationQueue(private val scope: CoroutineScope) {

    private class Entry(
        val presenter: ErrorPresenter,
        val params: ErrorPresenter.Params,
        val completion: ErrorPresenter.Completion,
    )

    private val pending = ArrayDeque<Entry>()
    private var inFlight: Entry? = null
    private var draining = false

    /** [presenter] as the SDK uses it: every presentation goes through this queue. */
    fun serialized(presenter: ErrorPresenter): ErrorPresenter = ErrorPresenter { params, completion ->
        pending.addLast(Entry(presenter, params, completion))
        drain()
    }

    // A presenter may complete, or enqueue, from inside present(); the loop picks that up instead of recursing.
    private fun drain() {
        if (draining) return
        draining = true
        try {
            while (inFlight == null) {
                val next = pending.removeFirstOrNull() ?: break
                inFlight = next
                present(next)
            }
        } finally {
            draining = false
        }
    }

    private fun present(entry: Entry) {
        val completed = AtomicBoolean(false)
        val completion = ErrorPresenter.Completion {
            if (completed.compareAndSet(false, true)) {
                entry.completion.complete()
                // Posted, never inline: the scope's main dispatcher is the non-immediate one, so the next error is
                // presented on a later message and not from inside the presenter's own complete().
                scope.launch { finished(entry) }
            }
        }
        try {
            entry.presenter.present(entry.params, completion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Nothing must wait on error UI that is not there (the SDK's own needs a started activity).
            Logger.e("Error presenter failed: $e")
            completion.complete()
        }
    }

    private fun finished(entry: Entry) {
        if (inFlight !== entry) return
        inFlight = null
        drain()
    }
}
