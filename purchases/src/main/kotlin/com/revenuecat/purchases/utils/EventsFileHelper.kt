package com.revenuecat.purchases.utils

import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject

/**
 * Class to handle file operations for event types like PaywallEvents and Diagnostics.
 * When [eventDeserializer] is null, [readFile] with the deserialized type won't return any events.
 */
internal open class EventsFileHelper<T : Event> (
    private val fileHelper: FileHelper,
    private val filePath: String,
    private val eventDeserializer: ((String) -> T)? = null,
) {
    @Synchronized
    fun appendEvent(event: T) {
        fileHelper.appendToFile(filePath, event.toString() + "\n")
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
            verboseLog("Failed to delete events file in $filePath.")
        }
    }

    private fun mapToEvent(string: String): T? {
        val eventDeserializer = eventDeserializer ?: return null
        return try {
            eventDeserializer(string)
        } catch (e: SerializationException) {
            errorLog("Error parsing event from file: $string", e)
            null
        } catch (e: IllegalArgumentException) {
            errorLog("Error parsing event from file: $string", e)
            null
        }
    }
}
