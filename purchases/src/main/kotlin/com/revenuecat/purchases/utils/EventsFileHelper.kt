@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.DebugEvent
import com.revenuecat.purchases.DebugEventName
import com.revenuecat.purchases.InternalRevenueCatAPI
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
    internal val filePath: String,
    private val eventSerializer: ((T) -> String)? = null,
    private val eventDeserializer: ((String) -> T)? = null,
) {
    var debugEventCallback: ((DebugEvent) -> Unit)? = null

    @Synchronized
    fun appendEvent(event: T) {
        try {
            fileHelper.appendToFile(
                filePath,
                (eventSerializer?.invoke(event) ?: event.toString()) + "\n",
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            debugEventCallback?.invoke(
                DebugEvent(
                    name = DebugEventName.APPEND_EVENT_EXCEPTION,
                    properties = mapOf("exceptionType" to (e::class.simpleName ?: "Unknown")),
                ),
            )
            throw e
        }
    }

    @Synchronized
    fun fileSizeInKB(): Double {
        return fileHelper.fileSizeInKB(filePath)
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
        try {
            fileHelper.removeFirstLinesFromFile(filePath, eventsToDeleteCount) { e ->
                debugEventCallback?.invoke(
                    DebugEvent(
                        name = DebugEventName.REMOVE_LINES_EXCEPTION,
                        properties = mapOf("exceptionType" to (e::class.simpleName ?: "Unknown")),
                    ),
                )
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            debugEventCallback?.invoke(
                DebugEvent(
                    name = DebugEventName.REMOVE_LINES_EXCEPTION,
                    properties = mapOf("exceptionType" to (e::class.simpleName ?: "Unknown")),
                ),
            )
        }
    }

    @Synchronized
    fun deleteFile() {
        if (!fileHelper.deleteFile(filePath)) {
            verboseLog { "Failed to delete events file in $filePath." }
        }
    }

    private fun mapToEvent(string: String): T? {
        val eventDeserializer = eventDeserializer
        if (eventDeserializer == null) {
            debugEventCallback?.invoke(
                DebugEvent(
                    name = DebugEventName.DESERIALIZATION_ERROR,
                    properties = emptyMap(),
                ),
            )
            return null
        }
        return try {
            eventDeserializer(string)
        } catch (e: SerializationException) {
            debugEventCallback?.invoke(
                DebugEvent(
                    name = DebugEventName.DESERIALIZATION_ERROR,
                    properties = mapOf("exceptionType" to "SerializationException"),
                ),
            )
            errorLog(e) { "Error parsing event from file: $string" }
            null
        } catch (e: IllegalArgumentException) {
            debugEventCallback?.invoke(
                DebugEvent(
                    name = DebugEventName.DESERIALIZATION_ERROR,
                    properties = mapOf("exceptionType" to "IllegalArgumentException"),
                ),
            )
            errorLog(e) { "Error parsing event from file: $string" }
            null
        }
    }
}
