package com.revenuecat.purchases.common.offlineentitlements

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.Topic
import com.revenuecat.purchases.common.remoteconfig.TopicFetcher
import com.revenuecat.purchases.common.verboseLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.json.JSONObject
import java.io.File

@OptIn(InternalRevenueCatAPI::class)
internal class ProductEntitlementMappingTopicReader(
    private val applicationContext: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lock = Any()

    @Volatile
    private var cached: ProductEntitlementMapping? = null
    private var inFlight: Deferred<ProductEntitlementMapping?>? = null

    suspend fun read(): ProductEntitlementMapping? {
        // Lazy start: assigning `inFlight` must happen before the body runs, otherwise an eager
        // dispatcher would let the body's `inFlight = null` execute and then `.also` would
        // overwrite it back with the (already-completed) deferred, leaking it into the next read.
        val deferred: Deferred<ProductEntitlementMapping?>? = synchronized(lock) {
            if (cached != null) {
                null
            } else {
                inFlight ?: scope.async(start = CoroutineStart.LAZY) {
                    val result = readMappingFromTopicFile()
                    synchronized(lock) {
                        cached = result
                        inFlight = null
                    }
                    result
                }.also { inFlight = it }
            }
        }
        return deferred?.await() ?: cached
    }

    fun invalidate() {
        synchronized(lock) {
            cached = null
        }
    }

    private fun readMappingFromTopicFile(): ProductEntitlementMapping? {
        val dir = File(
            File(applicationContext.noBackupFilesDir, TopicFetcher.TOPICS_ROOT),
            Topic.PRODUCT_ENTITLEMENT_MAPPING.key,
        )
        val blobFile = dir.takeIf { it.isDirectory }
            ?.listFiles()
            ?.firstOrNull { !it.name.startsWith(TopicFetcher.TEMP_PREFIX) }
        if (blobFile == null) {
            verboseLog { "No product entitlement mapping topic blob found at ${dir.absolutePath}" }
            return null
        }
        return runCatching {
            val json = JSONObject(blobFile.readBytes().decodeToString())
            ProductEntitlementMapping.fromJson(json, loadedFromCache = true)
        }.onFailure { e ->
            errorLog(e) { "Failed to parse product entitlement mapping topic file at ${blobFile.absolutePath}" }
        }.getOrNull()
    }
}
