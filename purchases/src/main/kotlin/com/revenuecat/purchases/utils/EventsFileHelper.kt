package com.revenuecat.purchases.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.verboseLog
import org.json.JSONObject
import java.util.stream.Stream

@RequiresApi(Build.VERSION_CODES.N)
internal open class EventsFileHelper<T : Event> (
    private val fileHelper: FileHelper,
    private val filePath: String,
    private val stringToEventConverter: ((String) -> T)? = null,
) {
    @Synchronized
    fun appendEvent(event: T) {
        fileHelper.appendToFile(filePath, event.toString() + "\n")
    }

    @Synchronized
    fun readFile(streamBlock: ((Stream<T>) -> Unit)) {
        val stringToEventConverter = stringToEventConverter
        if (stringToEventConverter == null || fileHelper.fileIsEmpty(filePath)) {
            streamBlock(Stream.empty())
        } else {
            fileHelper.readFilePerLines(filePath) { stream ->
                streamBlock(stream.map { stringToEventConverter(it) })
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
}
