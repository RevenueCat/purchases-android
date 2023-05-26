//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.caching

import android.content.SharedPreferences
import com.revenuecat.purchases.CustomerInfo
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
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import com.revenuecat.purchases.strings.ReceiptStrings
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.time.Duration.Companion.hours

private val PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD = 25.hours
private const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."
internal const val CUSTOMER_INFO_SCHEMA_VERSION = 3

open class DeviceCache(
    private val preferences: SharedPreferences,
    private val apiKey: String,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {
    companion object {
        private const val CUSTOMER_INFO_SCHEMA_VERSION_KEY = "schema_version"
        private const val CUSTOMER_INFO_VERIFICATION_RESULT_KEY = "verification_result"
        private const val CUSTOMER_INFO_REQUEST_DATE_KEY = "customer_info_request_date"
    }

    private val apiKeyPrefix: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey" }
    val legacyAppUserIDCacheKey: String by lazy { apiKeyPrefix }
    val appUserIDCacheKey: String by lazy { "$apiKeyPrefix.new" }
    internal val attributionCacheKey = "$SHARED_PREFERENCES_PREFIX.attribution"
    val tokensCacheKey: String by lazy { "$apiKeyPrefix.tokens" }

    private val productEntitlementMappingCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMapping"
    }
    private val productEntitlementMappingLastUpdatedCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMappingLastUpdated"
    }

    private val customerInfoCachesLastUpdatedCacheBaseKey: String by lazy {
        "$apiKeyPrefix.purchaserInfoLastUpdated"
    }

    private val offeringsResponseCacheKey: String by lazy { "$apiKeyPrefix.offeringsResponse" }

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
        getCustomerInfoCachesLastUpdated(appUserID).isStale(appInBackground, dateProvider)

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
    private fun getCustomerInfoCachesLastUpdated(appUserID: String): Date {
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

    @Synchronized
    fun getPreviouslySentHashedTokens(): Set<String> {
        return try {
            (preferences.getStringSet(tokensCacheKey, emptySet())?.toSet() ?: emptySet()).also {
                log(LogIntent.DEBUG, ReceiptStrings.TOKENS_ALREADY_POSTED.format(it))
            }
        } catch (e: ClassCastException) {
            emptySet()
        }
    }

    @Synchronized
    fun addSuccessfullyPostedToken(token: String) {
        log(LogIntent.DEBUG, ReceiptStrings.SAVING_TOKENS_WITH_HASH.format(token, token.sha1()))
        getPreviouslySentHashedTokens().let {
            log(LogIntent.DEBUG, ReceiptStrings.TOKENS_IN_CACHE.format(it))
            setSavedTokenHashes(it.toMutableSet().apply { add(token.sha1()) })
        }
    }

    @Synchronized
    private fun setSavedTokenHashes(newSet: Set<String>) {
        log(LogIntent.DEBUG, ReceiptStrings.SAVING_TOKENS.format(newSet))
        preferences.edit().putStringSet(tokensCacheKey, newSet).apply()
    }

    /**
     * Removes from the database all hashed tokens that are not considered active anymore, i.e. all
     * consumed in-apps or inactive subscriptions hashed tokens that are still in the local cache.
     */
    @Synchronized
    fun cleanPreviouslySentTokens(
        hashedTokens: Set<String>
    ) {
        log(LogIntent.DEBUG, ReceiptStrings.CLEANING_PREV_SENT_HASHED_TOKEN)
        setSavedTokenHashes(
            hashedTokens.intersect(getPreviouslySentHashedTokens())
        )
    }

    /**
     * Returns a list containing all tokens that are in [hashedTokens]
     * that are not present in the device cache.
     * In other words, returns all hashed tokens that are active and have not
     * been posted to our backend yet.
     */
    @Synchronized
    fun getActivePurchasesNotInCache(
        hashedTokens: Map<String, StoreTransaction>,
    ): List<StoreTransaction> {
        return hashedTokens
            .minus(getPreviouslySentHashedTokens())
            .values.toList()
    }

    // endregion

    // region offerings response

    @Synchronized
    fun getOfferingsResponseCache(): JSONObject? {
        return getJSONObjectOrNull(offeringsResponseCacheKey)
    }

    @Synchronized
    fun cacheOfferingsResponse(offeringsResponse: JSONObject) {
        preferences.edit()
            .putString(
                offeringsResponseCacheKey,
                offeringsResponse.toString()
            ).apply()
    }

    @Synchronized
    fun clearOfferingsResponseCache() {
        preferences.edit().remove(offeringsResponseCacheKey).apply()
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
        return getProductEntitlementMappingLastUpdated().isStale(
            PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD,
            dateProvider
        )
    }

    @Synchronized
    fun getProductEntitlementMapping(): ProductEntitlementMapping? {
        return preferences.getString(productEntitlementMappingCacheKey, null)?.let { jsonString ->
            return try {
                ProductEntitlementMapping.fromJson(JSONObject(jsonString))
            } catch (e: JSONException) {
                errorLog(OfflineEntitlementsStrings.ERROR_PARSING_PRODUCT_ENTITLEMENT_MAPPING.format(jsonString), e)
                preferences.edit().remove(productEntitlementMappingCacheKey).apply()
                null
            }
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
