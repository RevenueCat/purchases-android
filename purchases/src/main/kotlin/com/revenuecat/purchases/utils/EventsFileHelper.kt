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
    private val eventSerializer: ((T) -> String)? = null,
    private val eventDeserializer: ((String) -> T)? = null,
) {
    @Synchronized
    public fun appendEvent(event: T) {
        fileHelper.appendToFile(
            filePath,
            (eventSerializer?.invoke(event) ?: event.toString()) + "\n",
        )
    }

    @Synchronized
    public fun fileSizeInKB(): Double {
        return fileHelper.fileSizeInKB(filePath)
    }

    @Synchronized
    public fun readFile(block: ((Sequence<T?>) -> Unit)) {
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
    public fun readFileAsJson(block: ((Sequence<JSONObject>) -> Unit)) {
        if (fileHelper.fileIsEmpty(filePath)) {
            block(emptySequence())
        } else {
            fileHelper.readFilePerLines(filePath) { sequence ->
                block(sequence.map { JSONObject(it) })
            }
        }
    }

    @Synchronized
    public fun clear(eventsToDeleteCount: Int) {
        fileHelper.removeFirstLinesFromFile(filePath, eventsToDeleteCount)
    }

    @Synchronized
    public fun deleteFile() {
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
