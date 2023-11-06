package com.revenuecat.purchases.paywalls.events

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.FileHelper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.stream.Stream

@RequiresApi(Build.VERSION_CODES.N)
internal class PaywallEventsFileHelper(
    private val fileHelper: FileHelper,
) {
    companion object {
        const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
    }

    @Synchronized
    fun appendEvent(event: PaywallStoredEvent) {
        val eventString = PaywallEventsManager.json.encodeToString(event)
        fileHelper.appendToFile(PAYWALL_EVENTS_FILE_PATH, eventString + "\n")
    }

    @Synchronized
    fun readFile(streamBlock: ((Stream<PaywallStoredEvent>) -> Unit)) {
        if (fileHelper.fileIsEmpty(PAYWALL_EVENTS_FILE_PATH)) {
            streamBlock(Stream.empty())
        } else {
            fileHelper.readFilePerLines(PAYWALL_EVENTS_FILE_PATH) { stream ->
                streamBlock(stream.map { PaywallEventsManager.json.decodeFromString(it) })
            }
        }
    }

    @Synchronized
    fun clear(eventsToDeleteCount: Int) {
        fileHelper.removeFirstLinesFromFile(PAYWALL_EVENTS_FILE_PATH, eventsToDeleteCount)
    }
}
