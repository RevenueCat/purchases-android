package com.revenuecat.purchases.utils

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import org.json.JSONObject

/**
 * Class to handle file operations for event types like PaywallEvents and Diagnostics.
 * When [eventDeserializer] is null, [readFile] with the deserialized type won't return any events.
 */
internal open class EventsFileHelper<T : Event> (
    private val fileHelper: FileHelper,
    private val filePath: String,
    private val eventSerializer: ((T) -> String)? = null,
    private val eventDeserializer: ((String) -> T)? = null,
) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val FILE_SIZE_LIMIT_KB = 2048.0

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EVENTS_TO_CLEAR_ON_LIMIT = 50
    }

    @Synchronized
    fun appendEvent(event: T) {
        checkFileSizeAndClearIfNeeded()
        fileHelper.appendToFile(
            filePath,
            (eventSerializer?.invoke(event) ?: event.toString()) + "\n",
        )
    }

    private fun checkFileSizeAndClearIfNeeded() {
        val currentFileSizeKB = fileHelper.fileSizeInKB(filePath)
        if (currentFileSizeKB >= FILE_SIZE_LIMIT_KB) {
            warnLog { "Event store size limit reached. Clearing oldest events to free up space." }
            clear(EVENTS_TO_CLEAR_ON_LIMIT)
        }
    }

    @Synchronized
    fun readFile(block: ((Sequence<T?>) -> Unit)) {
        val eventDeserializer = eventDeserializer
        if (eventDeserializer == null || fileHelper.fileIsEmpty(filePath)) {
            block(emptySequence())
        } else {
            fileHelper.readFilePerLines(filePath) { sequence ->
                block(sequence.map { line -> mapToEvent(line) })
            }
        }
    }

    // Diagnostics currently doesn't require parsing the object back to the original model,
    // so adding this method to avoid the overhead of converting back to the model, then
    // back again to a JSONObject.
    @Synchronized
    fun readFileAsJson(block: ((Sequence<JSONObject>) -> Unit)) {
        if (fileHelper.fileIsEmpty(filePath)) {
            block(emptySequence())
        } else {
            fileHelper.readFilePerLines(filePath) { sequence ->
                block(sequence.map { JSONObject(it) })
            }
        }
    }

    @Synchronized
    fun clear(eventsToDeleteCount: Int) {
        fileHelper.removeFirstLinesFromFile(filePath, eventsToDeleteCount)
    }

    @Synchronized
    fun deleteFile() {
        if (!fileHelper.deleteFile(filePath)) {
            verboseLog { "Failed to delete events file in $filePath." }
        }
    }

    private fun mapToEvent(string: String): T? {
        val eventDeserializer = eventDeserializer ?: return null
        return try {
            eventDeserializer(string)
        } catch (e: SerializationException) {
            errorLog(e) { "Error parsing event from file: $string" }
            null
        } catch (e: IllegalArgumentException) {
            errorLog(e) { "Error parsing event from file: $string" }
            null
        }
    }
}
