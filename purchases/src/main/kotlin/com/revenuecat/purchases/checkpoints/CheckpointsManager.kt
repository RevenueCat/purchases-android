@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.errorLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates a checkpoint hit: fires listener events, resolves the checkpoint to a workflow through
 * [CheckpointWorkflowResolver], and runs the resolved workflow through [CheckpointWorkflowExecutor].
 */
internal class CheckpointsManager(
    private val resolver: CheckpointWorkflowResolver,
    private val executor: CheckpointWorkflowExecutor,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

    @get:Synchronized
    @set:Synchronized
    var checkpointListener: CheckpointListener? = null

    /**
     * Runs on the main dispatcher; listener callbacks fire on the main thread. For UI outcomes,
     * suspends until the presented workflow finishes.
     *
     * @throws PurchasesException if the checkpoint workflow should run but can't.
     */
    suspend fun checkpoint(identifier: String, params: CheckpointParams?): CheckpointResult =
        withContext(mainDispatcher) {
            val checkpoint = CheckpointInfo(identifier, params ?: CheckpointParams())
            checkpointListener?.onCheckpointHit(checkpoint)
            val result = when (val resolution = resolver.resolve(checkpoint)) {
                is CheckpointWorkflowResolution.Matched -> execute(resolution.presentation)
                is CheckpointWorkflowResolution.NoMatch ->
                    CheckpointResult.NoAction(checkpoint, resolution.reason)
                is CheckpointWorkflowResolution.Failed -> {
                    errorLog(resolution.error)
                    throw PurchasesException(resolution.error)
                }
            }
            checkpointListener?.onCheckpointResolved(checkpoint, result)
            result
        }

    private suspend fun execute(presentation: CheckpointWorkflowPresentation): CheckpointResult =
        when (val outcome = executor.execute(presentation)) {
            is CheckpointWorkflowOutcome.PaywallFinished -> {
                checkpointListener?.onCheckpointPaywallFinished(presentation.checkpoint, outcome.paywallOutcome)
                CheckpointResult.PaywallPresented(presentation.checkpoint, outcome.paywallOutcome)
            }
        }
}
