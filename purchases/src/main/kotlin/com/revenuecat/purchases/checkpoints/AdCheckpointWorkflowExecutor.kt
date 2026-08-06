@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import android.app.Activity
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.errorLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * POC [CheckpointWorkflowExecutor] for ad checkpoints — mirrors [UiCheckpointWorkflowExecutor]'s
 * ServiceLoader/CompletableDeferred pattern.
 */
internal class AdCheckpointWorkflowExecutor(
    private val currentActivityProvider: () -> Activity?,
    private val presenterProvider: () -> AdCheckpointPresenter? = ::loadPresenter,
) : CheckpointWorkflowExecutor, AdCheckpointPresenterDelegate {

    private val presenter: AdCheckpointPresenter? by lazy { presenterProvider() }
    private val pendingCalls = mutableMapOf<String, CompletableDeferred<CheckpointAdOutcome>>()
    private val presenting = AtomicBoolean(false)

    override suspend fun execute(presentation: CheckpointWorkflowPresentation): CheckpointWorkflowOutcome {
        val adUnitId = presentation.adUnitId ?: executionError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present ad checkpoint workflow: no adUnitId on the resolved presentation.",
        )
        val presenter = presenter ?: executionError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present ad checkpoint workflow: no AdCheckpointPresenter is registered.",
        )
        val activity = currentActivityProvider() ?: executionError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present ad checkpoint workflow: no started Activity found.",
        )
        if (!presenting.compareAndSet(false, true)) {
            executionError(
                PurchasesErrorCode.OperationAlreadyInProgressError,
                "Another checkpoint workflow is already being presented.",
            )
        }
        val adFinished = CompletableDeferred<CheckpointAdOutcome>()
        val callId = UUID.randomUUID().toString()
        synchronized(this) { pendingCalls[callId] = adFinished }
        try {
            presenter.present(activity, callId, adUnitId, this)
            return CheckpointWorkflowOutcome.AdFinished(adFinished.await())
        } catch (e: CancellationException) {
            abandonPendingCall(callId)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            abandonPendingCall(callId)
            executionError(
                PurchasesErrorCode.ConfigurationError,
                "Failed to present ad checkpoint workflow: $e",
            )
        }
    }

    override fun onCheckpointAdFinished(callId: String, adOutcome: CheckpointAdOutcome) {
        val pendingCall = synchronized(this) { pendingCalls.remove(callId) } ?: return
        presenting.set(false)
        pendingCall.complete(adOutcome)
    }

    private fun abandonPendingCall(callId: String) {
        synchronized(this) { pendingCalls.remove(callId) } ?: return
        presenting.set(false)
    }

    private fun executionError(code: PurchasesErrorCode, message: String): Nothing {
        val error = PurchasesError(code, message)
        errorLog(error)
        throw PurchasesException(error)
    }

    private companion object {
        fun loadPresenter(): AdCheckpointPresenter? = try {
            ServiceLoader.load(
                AdCheckpointPresenter::class.java,
                AdCheckpointPresenter::class.java.classLoader,
            ).firstOrNull()
        } catch (e: ServiceConfigurationError) {
            errorLog { "Error loading AdCheckpointPresenter: $e" }
            null
        }
    }
}
