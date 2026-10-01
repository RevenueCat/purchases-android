@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.responses.CustomerInfoResponseJsonKeys
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

/**
 * The copy of the subscriber's dimensions that the purchase response (POST `/receipts`) carries as the
 * `dimensions` and `as_of` siblings of `subscriber`. Persisted per app user in [DeviceCache] under the same
 * `{dimensions, as_of}` shape the `subscriber_dimensions` topic uses, and parsed once per stored value so a read
 * on the evaluation thread is a preference lookup plus a string comparison.
 */
internal class SubscriberDimensionsReceiptStore(
    private val deviceCache: DeviceCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val lock = Any()
    private var memoJson: String? = null
    private var memoDimensions: SubscriberDimensions? = null

    /**
     * Stores the dimensions [responseBody] carries for [appUserID]. A response without both `dimensions` and
     * `as_of` can't be ordered against the config copy, so it leaves the stored copy untouched.
     */
    fun store(appUserID: String, responseBody: JSONObject) {
        val dimensions = responseBody.optJSONObject(CustomerInfoResponseJsonKeys.DIMENSIONS)
        val asOf = responseBody.opt(CustomerInfoResponseJsonKeys.AS_OF) as? Number
        if (dimensions == null || asOf == null) {
            if (dimensions != null || asOf != null) {
                warnLog {
                    "Ignoring the purchase response's subscriber dimensions: it needs both 'dimensions' and 'as_of'."
                }
            }
            return
        }
        val json = JSONObject()
            .put(CustomerInfoResponseJsonKeys.DIMENSIONS, dimensions)
            .put(CustomerInfoResponseJsonKeys.AS_OF, asOf.toLong())
            .toString()
        val parsed = parse(json) ?: return
        synchronized(lock) {
            deviceCache.cacheSubscriberDimensions(appUserID, json)
            memoJson = json
            memoDimensions = parsed
        }
        debugLog {
            "Stored ${parsed.values.size} subscriber dimension(s) as of ${parsed.asOf} from the purchase response."
        }
    }

    /** The stored copy for [appUserID], or `null`. Never leaves the caller's thread. */
    fun get(appUserID: String): SubscriberDimensions? = synchronized(lock) {
        val json = deviceCache.getCachedSubscriberDimensionsJson(appUserID) ?: return null
        if (json !== memoJson && json != memoJson) {
            memoJson = json
            memoDimensions = parse(json)
        }
        memoDimensions
    }

    /**
     * Removes the stored copy once the config endpoint has served a newer one, off the caller's thread. A copy
     * stored in the meantime that is newer than [superseded] is kept.
     */
    fun discardAsync(appUserID: String, superseded: SubscriberDimensions) {
        scope.launch {
            synchronized(lock) {
                val stored = get(appUserID) ?: return@launch
                if (stored.asOf.after(superseded.asOf)) return@launch
                deviceCache.clearSubscriberDimensions(appUserID)
                memoJson = null
                memoDimensions = null
                verboseLog {
                    "Discarded the purchase response's subscriber dimensions (as of ${stored.asOf}): the config " +
                        "endpoint's copy is newer."
                }
            }
        }
    }

    private fun parse(json: String): SubscriberDimensions? {
        val item = try {
            JsonTools.json.parseToJsonElement(json) as? JsonObject
        } catch (e: SerializationException) {
            warnLog { "The stored subscriber dimensions can't be read, so they are ignored: $e" }
            return null
        }
        return when {
            item == null -> {
                warnLog { "The stored subscriber dimensions are not a JSON object, so they are ignored." }
                null
            }
            // Dimensions stored before `as_of` existed can't be ordered against the config copy.
            !item.containsKey(CustomerInfoResponseJsonKeys.AS_OF) -> {
                debugLog { "Ignoring subscriber dimensions stored without a timestamp by an earlier SDK version." }
                null
            }
            else -> SubscriberDimensions.parse(item)
        }
    }
}
