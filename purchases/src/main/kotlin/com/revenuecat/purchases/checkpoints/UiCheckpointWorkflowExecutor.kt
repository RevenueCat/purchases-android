@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import android.app.Activity
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.errorLog
import kotlinx.coroutines.CompletableDeferred
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs a checkpoint workflow by presenting it as UI through the [CheckpointPresenter] the RevenueCat UI module
 * registers via [ServiceLoader]. Owns the one-presentation-at-a-time constraint and the pending-call registry
 * that routes the presenter's terminal report back to the suspended [execute] call.
 */
internal class UiCheckpointWorkflowExecutor(
    private val currentActivityProvider: () -> Activity?,
    private val presenterProvider: () -> CheckpointPresenter? = ::loadPresenter,
) : CheckpointWorkflowExecutor, CheckpointPresenterDelegate {

    private val presenter: CheckpointPresenter? by lazy { presenterProvider() }
    private val pendingCalls = mutableMapOf<String, CompletableDeferred<CheckpointPaywallResult>>()
    private val presenting = AtomicBoolean(false)

    override suspend fun execute(presentation: CheckpointWorkflowPresentation): CheckpointWorkflowOutcome {
        val presenter = presenter ?: executionError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: the RevenueCat UI module is not present.",
        )
        val activity = currentActivityProvider() ?: executionError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: no started Activity found.",
        )
        if (!presenting.compareAndSet(false, true)) {
            executionError(
                PurchasesErrorCode.OperationAlreadyInProgressError,
                "Another checkpoint workflow is already being presented.",
            )
        }
        val paywallFinished = CompletableDeferred<CheckpointPaywallResult>()
        val callId = UUID.randomUUID().toString()
        synchronized(this) { pendingCalls[callId] = paywallFinished }
        presenter.present(activity, callId, presentation, this)
        return CheckpointWorkflowOutcome.PaywallFinished(paywallFinished.await())
    }

    override fun onCheckpointPaywallFinished(callId: String, paywallResult: CheckpointPaywallResult) {
        val pendingCall = synchronized(this) { pendingCalls.remove(callId) } ?: return
        presenting.set(false)
        pendingCall.complete(paywallResult)
    }

    private fun executionError(code: PurchasesErrorCode, message: String): Nothing {
        val error = PurchasesError(code, message)
        errorLog(error)
        throw PurchasesException(error)
    }

    private companion object {
        fun loadPresenter(): CheckpointPresenter? = try {
            ServiceLoader.load(
                CheckpointPresenter::class.java,
                CheckpointPresenter::class.java.classLoader,
            ).firstOrNull()
        } catch (e: ServiceConfigurationError) {
            errorLog { "Error loading CheckpointPresenter: $e" }
            null
        }
    }
}
