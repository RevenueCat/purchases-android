package com.revenuecat.purchases.paywalls.events

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.identity.IdentityManager
import kotlinx.serialization.json.Json

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RequiresApi(Build.VERSION_CODES.N)
internal class PaywallEventsManager(
    private val fileHelper: PaywallEventsFileHelper,
    private val identityManager: IdentityManager,
    private val paywallEventsDispatcher: Dispatcher,
) {

    companion object {
        internal val json = Json.Default
    }

    fun track(event: PaywallEvent) {
        enqueue {
            debugLog("Tracking paywall event: $event")
            fileHelper.appendEvent(PaywallStoredEvent(event, identityManager.currentAppUserID))
        }
    }

    fun flushEvents() {
        // WIP
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        paywallEventsDispatcher.enqueue({
            command()
        }, delay)
    }
}
