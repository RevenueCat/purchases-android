package com.revenuecat.checkpointtester.checkpoints

import android.util.Log
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointCompletedContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointHitContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Example of the app-wide analytics integration [CheckpointListener] is meant for: it observes every checkpoint
 * hit and its result regardless of which screen triggered it. Registered in
 * [com.revenuecat.checkpointtester.MainApplication] and rendered by the listener log screen.
 */
@OptIn(InternalRevenueCatAPI::class)
object CheckpointEventLog : CheckpointListener {

    private const val TAG = "CheckpointEventLog"
    private const val MAX_EVENTS = 100
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    override fun onCheckpointHit(context: OnCheckpointHitContext) {
        track("Hit · ${context.identifier} · customVariables=${context.customVariables}")
    }

    override fun onCheckpointCompleted(context: OnCheckpointCompletedContext) {
        track("Completed · ${context.identifier} · ${describe(context.result)}")
    }

    fun clear() {
        _events.update { emptyList() }
    }

    private fun describe(result: CheckpointResult): String = when (result) {
        is CheckpointResult.ReceivedOffering -> "Offering (${result.offering.identifier})"
        is CheckpointResult.PaywallPresented -> when (val outcome = result.paywallOutcome) {
            is CheckpointPaywallOutcome.Purchased -> "Purchased"
            is CheckpointPaywallOutcome.Restored -> "Restored"
            CheckpointPaywallOutcome.Dismissed -> "Dismissed"
            CheckpointPaywallOutcome.WebCheckoutOpened -> "Web checkout opened"
            is CheckpointPaywallOutcome.Error -> "Paywall error: ${outcome.error.message}"
            else -> "Unknown outcome"
        }
        is CheckpointResult.NoAction -> "No action (${result.reason})"
        else -> "Unknown result"
    }

    private fun track(event: String) {
        Log.d(TAG, event)
        val timestamped = "${timeFormat.format(Date())}  $event"
        _events.update { (listOf(timestamped) + it).take(MAX_EVENTS) }
    }
}
