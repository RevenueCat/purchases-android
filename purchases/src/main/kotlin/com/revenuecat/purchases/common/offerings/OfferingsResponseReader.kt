package com.revenuecat.purchases.common.offerings

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter

internal class OfferingsResponse(
    val json: JSONObject,
    val omittedPaywallComponents: Boolean,
) {
    /**
     * [json] as text. Not [JSONObject.toString], which writes a whole-number double as an integer: the
     * metadata values developers read back from this text would change type.
     */
    fun serialize(): String {
        val text = StringWriter()
        JsonWriter(text).use { writer ->
            // A number that is not finite is written as it is rather than refused: this never throws.
            writer.isLenient = true
            writer.writeJson(json)
        }
        return text.toString()
    }
}

private fun JsonWriter.writeJson(value: Any?) {
    when (value) {
        is JSONObject -> {
            beginObject()
            value.keys().forEach { name ->
                name(name)
                writeJson(value.opt(name))
            }
            endObject()
        }
        is JSONArray -> {
            beginArray()
            for (index in 0 until value.length()) writeJson(value.opt(index))
            endArray()
        }
        is Number -> value(value)
        is Boolean -> value(value)
        is String -> value(value)
        else -> nullValue()
    }
}

/**
 * Reads an offerings response into the [JSONObject] the offering parsers consume, without building each
 * offering's `paywall_components` tree: its nested objects and arrays are left empty, which keeps the
 * top-level keys the parsers check for. That tree is most of a large response, and the parsers never read
 * it because components are served from `/v1/config`.
 */
internal object OfferingsResponseReader {

    @Throws(JSONException::class)
    fun read(payload: String): OfferingsResponse = read(StringReader(payload))

    @Throws(JSONException::class)
    fun read(reader: Reader): OfferingsResponse {
        return try {
            JsonReader(reader).use { jsonReader ->
                // As tolerant as the JSONObject(String) parsing this replaces.
                jsonReader.isLenient = true
                val reading = Reading(jsonReader)
                OfferingsResponse(reading.readResponse(), reading.omittedPaywallComponents)
            }
        } catch (e: IOException) {
            throw malformedResponse(e)
        } catch (e: IllegalStateException) {
            throw malformedResponse(e)
        }
    }

    /**
     * Decodes a response [body] into the text to hand on and to cache: without the contents of
     * `paywall_components` when it has any, and as it was received otherwise, which includes a body that
     * can't be parsed. The full body is only decoded to a String in that second case.
     */
    fun compact(body: ByteArray): String {
        val response = if (body.isEmpty()) null else readOrNull(body)
        return if (response?.omittedPaywallComponents == true) response.serialize() else String(body, Charsets.UTF_8)
    }

    @Suppress("SwallowedException")
    private fun readOrNull(body: ByteArray): OfferingsResponse? {
        return try {
            read(InputStreamReader(ByteArrayInputStream(body), Charsets.UTF_8))
        } catch (e: JSONException) {
            null
        }
    }

    private fun malformedResponse(cause: Exception): JSONException =
        JSONException("Malformed offerings response: ${cause.message}").apply { initCause(cause) }

    private class Reading(private val reader: JsonReader) {
        var omittedPaywallComponents = false
            private set

        fun readResponse(): JSONObject = readObject { name ->
            if (name == OFFERINGS && reader.peek() == JsonToken.BEGIN_ARRAY) readArray(::readOffering) else readValue()
        }

        private fun readOffering(): Any {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) return readValue()
            return readObject { name ->
                if (name == PAYWALL_COMPONENTS && reader.peek() == JsonToken.BEGIN_OBJECT) {
                    readObject { readValueOmittingNestedContents() }
                } else {
                    readValue()
                }
            }
        }

        private fun readValueOmittingNestedContents(): Any = when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JSONObject().also {
                reader.beginObject()
                while (reader.hasNext()) {
                    omittedPaywallComponents = true
                    reader.nextName()
                    reader.skipValue()
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JSONArray().also {
                reader.beginArray()
                while (reader.hasNext()) {
                    omittedPaywallComponents = true
                    reader.skipValue()
                }
                reader.endArray()
            }
            else -> readValue()
        }

        private fun readValue(): Any = when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> readObject { readValue() }
            JsonToken.BEGIN_ARRAY -> readArray(::readValue)
            JsonToken.NUMBER -> readNumber()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> JSONObject.NULL.also { reader.nextNull() }
            else -> reader.nextString()
        }

        private inline fun readObject(readMember: (name: String) -> Any): JSONObject {
            val result = JSONObject()
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                result.put(name, readMember(name))
            }
            reader.endObject()
            return result
        }

        private inline fun readArray(readElement: () -> Any): JSONArray {
            val result = JSONArray()
            reader.beginArray()
            while (reader.hasNext()) {
                result.put(readElement())
            }
            reader.endArray()
            return result
        }

        // Same types JSONTokener produces, which reach developers through Offering.metadata.
        private fun readNumber(): Any {
            val literal = reader.nextString()
            val integral = if ('.' in literal) null else literal.toLongOrNull()
            return when {
                integral == null -> literal.toDoubleOrNull() ?: literal
                integral in Int.MIN_VALUE..Int.MAX_VALUE -> integral.toInt()
                else -> integral
            }
        }
    }

    private const val OFFERINGS = "offerings"
    private const val PAYWALL_COMPONENTS = "paywall_components"
}
