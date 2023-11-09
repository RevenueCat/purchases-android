package com.revenuecat.purchases.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject
import java.util.stream.Stream

@RequiresApi(Build.VERSION_CODES.N)
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
    fun readFile(streamBlock: ((Stream<T>) -> Unit)) {
        val eventDeserializer = eventDeserializer
        if (eventDeserializer == null || fileHelper.fileIsEmpty(filePath)) {
            streamBlock(Stream.empty())
        } else {
            fileHelper.readFilePerLines(filePath) { stream ->
                streamBlock(stream.map { line -> mapToEvent(line) })
            }
        }
    }

    // Diagnostics currently doesn't require parsing the object back to the original model,
    // so adding this method to avoid the overhead of converting back to the model, then
    // back again to a JSONObject.
    @Synchronized
    fun readFileAsJson(streamBlock: ((Stream<JSONObject>) -> Unit)) {
        if (fileHelper.fileIsEmpty(filePath)) {
            streamBlock(Stream.empty())
        } else {
            fileHelper.readFilePerLines(filePath) { stream ->
                streamBlock(stream.map { JSONObject(it) })
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
