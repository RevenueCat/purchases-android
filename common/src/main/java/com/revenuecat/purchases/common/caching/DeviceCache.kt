//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.caching

import android.content.SharedPreferences
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.ReceiptStrings
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val CACHE_REFRESH_PERIOD_IN_FOREGROUND = 5.minutes
private val CACHE_REFRESH_PERIOD_IN_BACKGROUND = 25.hours
private val PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD = 25.hours
private const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."
internal const val CUSTOMER_INFO_SCHEMA_VERSION = 3

open class DeviceCache(
    private val preferences: SharedPreferences,
    private val apiKey: String,
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(),
    private val dateProvider: DateProvider = DefaultDateProvider()
) {
    companion object {
        private const val CUSTOMER_INFO_SCHEMA_VERSION_KEY = "schema_version"
        private const val CUSTOMER_INFO_VERIFICATION_RESULT_KEY = "verification_result"
        private const val CUSTOMER_INFO_REQUEST_DATE_KEY = "customer_info_request_date"
    }

    private val apiKeyPrefix: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey" }
    val legacyAppUserIDCacheKey: String by lazy { "$apiKeyPrefix" }
    val appUserIDCacheKey: String by lazy { "$apiKeyPrefix.new" }
    internal val attributionCacheKey = "$SHARED_PREFERENCES_PREFIX.attribution"
    val tokensCacheKey: String by lazy { "$apiKeyPrefix.tokens" }
    val orderIdsPerTokenCacheKey: String by lazy { "$apiKeyPrefix.orderids_per_token" }

    private val productEntitlementMappingCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMapping"
    }
    private val productEntitlementMappingLastUpdatedCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMappingLastUpdated"
    }

    private val customerInfoCachesLastUpdatedCacheBaseKey: String by lazy {
        "$apiKeyPrefix.purchaserInfoLastUpdated"
    }

    // region app user id

    @Synchronized
    fun getLegacyCachedAppUserID(): String? = preferences.getString(legacyAppUserIDCacheKey, null)

    @Synchronized
    fun getCachedAppUserID(): String? = preferences.getString(appUserIDCacheKey, null)

    @Synchronized
    fun cacheAppUserID(appUserID: String) {
        preferences.edit().putString(appUserIDCacheKey, appUserID).apply()
    }

    @Synchronized
    fun clearCachesForAppUserID(appUserID: String) {
        preferences.edit()
            .clearCustomerInfo()
            .clearAppUserID()
            .clearCustomerInfoCacheTimestamp(appUserID)
            .apply()
        clearOfferingsCache()
    }

    private fun SharedPreferences.Editor.clearCustomerInfo(): SharedPreferences.Editor {
        getCachedAppUserID()?.let {
            remove(customerInfoCacheKey(it))
        }
        getLegacyCachedAppUserID()?.let {
            remove(customerInfoCacheKey(it))
        }
        return this
    }

    private fun SharedPreferences.Editor.clearAppUserID(): SharedPreferences.Editor {
        remove(appUserIDCacheKey)
        remove(legacyAppUserIDCacheKey)
        return this
    }

    private fun SharedPreferences.Editor.clearCustomerInfoCacheTimestamp(appUserID: String): SharedPreferences.Editor {
        remove(customerInfoLastUpdatedCacheKey(appUserID))
        return this
    }

    // endregion

    // region purchaser info
    fun customerInfoCacheKey(appUserID: String) = "$legacyAppUserIDCacheKey.$appUserID"

    fun customerInfoLastUpdatedCacheKey(appUserID: String) = "$customerInfoCachesLastUpdatedCacheBaseKey.$appUserID"

    fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return preferences.getString(customerInfoCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    val cachedJSONObject = JSONObject(json)
                    val schemaVersion = cachedJSONObject.optInt(CUSTOMER_INFO_SCHEMA_VERSION_KEY)
                    val verificationResultString = if (cachedJSONObject.has(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)) {
                        cachedJSONObject.getString(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)
                    } else VerificationResult.NOT_REQUESTED.name
                    val requestDate = cachedJSONObject.optLong(CUSTOMER_INFO_REQUEST_DATE_KEY).takeIf { it > 0 }?.let {
                        Date(it)
                    }
                    cachedJSONObject.remove(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)
                    cachedJSONObject.remove(CUSTOMER_INFO_REQUEST_DATE_KEY)
                    val verificationResult = VerificationResult.valueOf(verificationResultString)
                    return if (schemaVersion == CUSTOMER_INFO_SCHEMA_VERSION) {
                        CustomerInfoFactory.buildCustomerInfo(cachedJSONObject, requestDate, verificationResult)
                    } else {
                        null
                    }
                } catch (e: JSONException) {
                    null
                }
            }
    }

    @Synchronized
    fun cacheCustomerInfo(appUserID: String, info: CustomerInfo) {
        val jsonObject = info.rawData.also {
            it.put(CUSTOMER_INFO_SCHEMA_VERSION_KEY, CUSTOMER_INFO_SCHEMA_VERSION)
            // Trusted entitlements: Commented out until ready to be made public
            // it.put(CUSTOMER_INFO_VERIFICATION_RESULT_KEY, info.entitlements.verification.name)
            it.put(CUSTOMER_INFO_REQUEST_DATE_KEY, info.requestDate.time)
        }
        preferences.edit()
            .putString(
                customerInfoCacheKey(appUserID),
                jsonObject.toString()
            ).apply()

        setCustomerInfoCacheTimestampToNow(appUserID)
    }

    @Synchronized
    fun isCustomerInfoCacheStale(appUserID: String, appInBackground: Boolean) =
        getCustomerInfoCachesLastUpdated(appUserID).isStale(appInBackground)

    @Synchronized
    fun clearCustomerInfoCacheTimestamp(appUserID: String) {
        preferences.edit().clearCustomerInfoCacheTimestamp(appUserID).apply()
    }

    @Synchronized
    fun clearCustomerInfoCache(appUserID: String) {
        val editor = preferences.edit()
        editor.clearCustomerInfoCacheTimestamp(appUserID)
        editor.remove(customerInfoCacheKey(appUserID))
        editor.apply()
    }

    @Synchronized
    fun setCustomerInfoCacheTimestampToNow(appUserID: String) {
        setCustomerInfoCacheTimestamp(appUserID, dateProvider.now)
    }

    @Synchronized
    fun setCustomerInfoCacheTimestamp(appUserID: String, date: Date) {
        preferences.edit().putLong(customerInfoLastUpdatedCacheKey(appUserID), date.time).apply()
    }

    @Synchronized
    fun getCustomerInfoCachesLastUpdated(appUserID: String): Date? {
        return Date(preferences.getLong(customerInfoLastUpdatedCacheKey(appUserID), 0))
    }

    // endregion

    // region attribution data

    @Synchronized
    fun cleanupOldAttributionData() {
        val editor = preferences.edit()
        for (key in preferences.all.keys) {
            if (key != null && key.startsWith(attributionCacheKey)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    // endregion

    // region purchase tokens

    /**
     * Migrates data from hashed tokens cache to cache with order IDs. If there are no hashed tokens in the cache, or if
     * there are already order IDs in the cache, this method does nothing. If there are hashed tokens in the cache, it
     * will migrate them to the cache with order IDs (setting an empty order ID), and remove the hashed tokens cache.
     *
     * This method is synchronized to ensure thread safety.
     */
    @Synchronized
    fun migrateHashedTokensCacheToCacheWithOrderIds() {
        val hashedTokens = getPreviouslySentHashedTokens()
        val hashedTokensAndOrderIds = getPreviouslySentOrderIdsPerHashToken()

        if (hashedTokens.isNotEmpty() && hashedTokensAndOrderIds.isEmpty()) {
            val orderIdsPerHashedToken = mutableMapOf<String, String>()
            hashedTokens.forEach { hashedToken ->
                orderIdsPerHashedToken[hashedToken] = ""
            }

            val editor = setSavedOrderIdsPerTokenHashes(orderIdsPerHashedToken, applyEditor = false)
            editor.remove(tokensCacheKey)
            editor.apply()
        }
    }

    @Synchronized
    fun getPreviouslySentOrderIdsPerHashToken(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val jsonString = preferences.getString(orderIdsPerTokenCacheKey, "{}") ?: "{}"
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getString(key)
            }
            log(LogIntent.DEBUG, ReceiptStrings.TOKENS_ALREADY_POSTED.format(map))
        } catch (e: JSONException) {
            errorLog(ReceiptStrings.ERROR_READING_TOKENS_FROM_CACHE, e)
        } catch (e: ClassCastException) {
            errorLog(ReceiptStrings.ERROR_READING_TOKENS_FROM_CACHE, e)
        }
        return map
    }

    @Synchronized
    fun addSuccessfullyPostedPurchase(token: String, orderId: String?) {
        log(LogIntent.DEBUG, ReceiptStrings.SAVING_ORDER_IDS_PER_TOKENS_WITH_HASH.format(orderId, token, token.sha1()))
        getPreviouslySentOrderIdsPerHashToken().let { storedMap ->
            log(LogIntent.DEBUG, ReceiptStrings.TOKENS_IN_CACHE.format(storedMap))
            storedMap.toMutableMap().apply { put(token.sha1(), orderId ?: "") }.also { newMap ->
                setSavedOrderIdsPerTokenHashes(newMap)
            }
        }
    }

    /**
     * Removes from the database all hashed tokens that are not considered active anymore, i.e. all
     * consumed in-apps or inactive subscriptions hashed tokens that are still in the local cache.
     *
     * It also adds order ids to the cache if the token is still active and the order id is not present. This can happen
     * after a migration from hashed tokens cache to order ids per hash tokens cache.
     */
    @Synchronized
    fun cleanUpTokensCache(
        activePurchasesByHashedToken: Map<String, StoreTransaction>
    ) {
        log(LogIntent.DEBUG, ReceiptStrings.CLEANING_PREV_SENT_HASHED_TOKEN)
        val tokensInCache = getPreviouslySentOrderIdsPerHashToken()
        val tokensInCacheThatAreStillActive = tokensInCache.filter {
            activePurchasesByHashedToken.contains(it.key)
        }
        val activeTokensWithFixedOrders =
            fixEmptyOrderIds(tokensInCacheThatAreStillActive, activePurchasesByHashedToken)
        if (tokensInCache != activeTokensWithFixedOrders) {
            setSavedOrderIdsPerTokenHashes(activeTokensWithFixedOrders)
        }
    }

    /**
     * Returns a list containing all tokens that are in [activePurchasesByHashedToken]
     * that are not present in the device cache.
     * In other words, returns all hashed tokens that are active and have not
     * been posted to our backend yet.
     */
    @Synchronized
    fun getActivePurchasesNotInCache(
        activePurchasesByHashedToken: Map<String, StoreTransaction>,
    ): List<StoreTransaction> {
        getPreviouslySentOrderIdsPerHashToken().let { storedTokensToOrderIds ->
            log(LogIntent.DEBUG, ReceiptStrings.TOKENS_IN_CACHE.format(storedTokensToOrderIds))
            return activePurchasesByHashedToken
                .minus(storedTokensToOrderIds.keys.toSet())
                .values.toList()
        }
    }

    private fun fixEmptyOrderIds(
        tokensInCacheThatAreStillActive: Map<String, String>,
        activePurchasesByHashedToken: Map<String, StoreTransaction>
    ): Map<String, String> {
        val mutableMap = tokensInCacheThatAreStillActive.toMutableMap()
        val activeTokensWithEmptyOrderId = tokensInCacheThatAreStillActive.filter { it.value.isEmpty() }
        activeTokensWithEmptyOrderId.forEach { (tokenHash, _) ->
            log(LogIntent.DEBUG, ReceiptStrings.EMPTY_ORDER_ID_DETECTED.format(tokenHash))
            activePurchasesByHashedToken[tokenHash]?.let { storeTransaction ->
                storeTransaction.orderId?.let { orderIdFromTransaction ->
                    log(
                        LogIntent.DEBUG,
                        ReceiptStrings.SAVING_ORDER_IDS_FOR_HASH.format(orderIdFromTransaction, tokenHash)
                    )
                    mutableMap[tokenHash] = orderIdFromTransaction
                }
            }
        }
        return mutableMap.toMap()
    }

    @Synchronized
    private fun setSavedOrderIdsPerTokenHashes(
        newMap: Map<String, String>,
        applyEditor: Boolean = true
    ): SharedPreferences.Editor {
        log(LogIntent.DEBUG, ReceiptStrings.SAVING_TOKENS.format(newMap))
        val jsonString = JSONObject(newMap).toString()
        val editor = preferences.edit()
        editor.putString(orderIdsPerTokenCacheKey, jsonString)
        if (applyEditor) {
            editor.apply()
        }
        return editor
    }

    @Synchronized
    internal fun getPreviouslySentHashedTokens(): Set<String> {
        return try {
            (preferences.getStringSet(tokensCacheKey, emptySet())?.toSet() ?: emptySet()).also {
                log(LogIntent.DEBUG, ReceiptStrings.TOKENS_ALREADY_POSTED.format(it))
            }
        } catch (e: ClassCastException) {
            emptySet()
        }
    }

    // endregion

    // region offerings

    val cachedOfferings: Offerings?
        get() = offeringsCachedObject.cachedInstance

    @Synchronized
    fun cacheOfferings(offerings: Offerings) {
        offeringsCachedObject.cacheInstance(offerings)
    }

    @Synchronized
    fun isOfferingsCacheStale(appInBackground: Boolean) = offeringsCachedObject.lastUpdatedAt.isStale(appInBackground)

    @Synchronized
    fun clearOfferingsCacheTimestamp() {
        offeringsCachedObject.clearCacheTimestamp()
    }

    @Synchronized
    fun setOfferingsCacheTimestampToNow() {
        offeringsCachedObject.updateCacheTimestamp(dateProvider.now)
    }

    private fun clearOfferingsCache() {
        offeringsCachedObject.clearCache()
    }

    // endregion

    // region ProductEntitlementMapping

    @Synchronized
    fun cacheProductEntitlementMapping(productEntitlementMapping: ProductEntitlementMapping) {
        preferences.edit()
            .putString(
                productEntitlementMappingCacheKey,
                productEntitlementMapping.toJson().toString()
            ).apply()

        setProductEntitlementMappingCacheTimestampToNow()
    }

    @Synchronized
    fun setProductEntitlementMappingCacheTimestampToNow() {
        setProductEntitlementMappingCacheTimestamp(dateProvider.now)
    }

    private fun setProductEntitlementMappingCacheTimestamp(date: Date) {
        preferences.edit().putLong(productEntitlementMappingLastUpdatedCacheKey, date.time).apply()
    }

    @Synchronized
    fun isProductEntitlementMappingCacheStale(): Boolean {
        return getProductEntitlementMappingLastUpdated().isStale(PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD)
    }

    @Synchronized
    fun getProductEntitlementMapping(): ProductEntitlementMapping? {
        return preferences.getString(productEntitlementMappingCacheKey, null)?.let { jsonString ->
            return ProductEntitlementMapping.fromJson(JSONObject(jsonString))
        }
    }

    private fun getProductEntitlementMappingLastUpdated(): Date? {
        return if (preferences.contains(productEntitlementMappingLastUpdatedCacheKey)) {
            Date(preferences.getLong(productEntitlementMappingLastUpdatedCacheKey, -1))
        } else {
            null
        }
    }

    // endregion

    // region utils

    private fun Date?.isStale(appInBackground: Boolean): Boolean {
        return this?.let {
            log(LogIntent.DEBUG, ReceiptStrings.CHECKING_IF_CACHE_STALE.format(appInBackground))
            val cacheDuration = when {
                appInBackground -> CACHE_REFRESH_PERIOD_IN_BACKGROUND
                else -> CACHE_REFRESH_PERIOD_IN_FOREGROUND
            }

            isStale(cacheDuration)
        } ?: true
    }

    private fun Date?.isStale(cacheDuration: Duration): Boolean {
        return this?.let { cacheLastUpdated ->
            (dateProvider.now.time - cacheLastUpdated.time).milliseconds >= cacheDuration
        } ?: true
    }

    open fun getJSONObjectOrNull(key: String): JSONObject? {
        return preferences.getString(key, null)?.let { json ->
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                null
            }
        }
    }

    open fun putString(
        cacheKey: String,
        value: String
    ) {
        preferences.edit().putString(
            cacheKey,
            value
        ).apply()
    }

    fun remove(
        cacheKey: String
    ) {
        preferences.edit().remove(cacheKey).apply()
    }

    fun findKeysThatStartWith(
        cacheKey: String
    ): Set<String> {
        return try {
            preferences.all
                ?.filterKeys { it.startsWith(cacheKey) }
                ?.keys ?: emptySet()
        } catch (e: NullPointerException) {
            emptySet()
        }
    }

    fun newKey(
        key: String
    ) = "$apiKeyPrefix.$key"

    // endregion
}
