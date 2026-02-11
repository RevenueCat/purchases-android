//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.caching

import android.content.SharedPreferences
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoOriginalSource
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.interfaces.StorefrontProvider
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import com.revenuecat.purchases.strings.ReceiptStrings
import com.revenuecat.purchases.strings.VirtualCurrencyStrings
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrenciesFactory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.time.Duration.Companion.hours

private val PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD = 25.hours
private const val SHARED_PREFERENCES_PREFIX = "com.revenuecat.purchases."
internal const val CUSTOMER_INFO_SCHEMA_VERSION = 3

@Suppress("TooManyFunctions")
internal open class DeviceCache(
    private val preferences: SharedPreferences,
    private val apiKey: String,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) : StorefrontProvider {
    public companion object {
        private const val CUSTOMER_INFO_SCHEMA_VERSION_KEY = "schema_version"
        private const val CUSTOMER_INFO_VERIFICATION_RESULT_KEY = "verification_result"
        private const val CUSTOMER_INFO_REQUEST_DATE_KEY = "customer_info_request_date"
        private const val CUSTOMER_INFO_ORIGINAL_SOURCE_KEY = "customer_info_original_source"
    }

    private val apiKeyPrefix: String by lazy { "$SHARED_PREFERENCES_PREFIX$apiKey" }
    public val legacyAppUserIDCacheKey: String by lazy { apiKeyPrefix }
    public val appUserIDCacheKey: String by lazy { "$apiKeyPrefix.new" }
    internal val attributionCacheKey = "$SHARED_PREFERENCES_PREFIX.attribution"
    public val tokensCacheKey: String by lazy { "$apiKeyPrefix.tokens" }
    public val storefrontCacheKey: String by lazy { "storefrontCacheKey" }

    private val productEntitlementMappingCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMapping"
    }
    private val productEntitlementMappingLastUpdatedCacheKey: String by lazy {
        "$apiKeyPrefix.productEntitlementMappingLastUpdated"
    }

    private val customerInfoCachesLastUpdatedCacheBaseKey: String by lazy {
        "$apiKeyPrefix.purchaserInfoLastUpdated"
    }

    private val virtualCurrenciesCacheBaseKey: String by lazy {
        "$apiKeyPrefix.virtualCurrencies"
    }

    private val virtualCurrenciesLastUpdatedCacheBaseKey: String by lazy {
        "$apiKeyPrefix.virtualCurrenciesLastUpdated"
    }

    private val offeringsResponseCacheKey: String by lazy { "$apiKeyPrefix.offeringsResponse" }

    public fun startEditing(): SharedPreferences.Editor {
        return preferences.edit()
    }

    // region app user id

    @Synchronized
    public fun getLegacyCachedAppUserID(): String? = preferences.getString(legacyAppUserIDCacheKey, null)

    @Synchronized
    public fun getCachedAppUserID(): String? = preferences.getString(appUserIDCacheKey, null)

    @Synchronized
    public fun cacheAppUserID(appUserID: String) {
        cacheAppUserID(appUserID, preferences.edit()).apply()
    }

    @Synchronized
    public fun cacheAppUserID(
        appUserID: String,
        cacheEditor: SharedPreferences.Editor,
    ): SharedPreferences.Editor {
        return cacheEditor.putString(appUserIDCacheKey, appUserID)
    }

    @Synchronized
    public fun clearCachesForAppUserID(appUserID: String) {
        preferences.edit()
            .clearCustomerInfo()
            .clearAppUserID()
            .clearCustomerInfoCacheTimestamp(appUserID)
            .clearVirtualCurrenciesCacheTimestamp(appUserID)
            .clearVirtualCurrenciesCache(appUserID)
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
    public fun customerInfoCacheKey(appUserID: String) = "$legacyAppUserIDCacheKey.$appUserID"

    public fun customerInfoLastUpdatedCacheKey(appUserID: String) =
        "$customerInfoCachesLastUpdatedCacheBaseKey.$appUserID"

    @Suppress
    public fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return preferences.getString(customerInfoCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    val cachedJSONObject = JSONObject(json)
                    val schemaVersion = cachedJSONObject.optInt(CUSTOMER_INFO_SCHEMA_VERSION_KEY)
                    val verificationResultString = if (cachedJSONObject.has(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)) {
                        cachedJSONObject.getString(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)
                    } else {
                        VerificationResult.NOT_REQUESTED.name
                    }
                    val requestDate = cachedJSONObject.optLong(CUSTOMER_INFO_REQUEST_DATE_KEY).takeIf { it > 0 }?.let {
                        Date(it)
                    }
                    val originalSourceString = cachedJSONObject.optNullableString(CUSTOMER_INFO_ORIGINAL_SOURCE_KEY)
                    val originalSource = CustomerInfoOriginalSource.fromString(originalSourceString)
                    cachedJSONObject.remove(CUSTOMER_INFO_VERIFICATION_RESULT_KEY)
                    cachedJSONObject.remove(CUSTOMER_INFO_REQUEST_DATE_KEY)
                    cachedJSONObject.remove(CUSTOMER_INFO_ORIGINAL_SOURCE_KEY)
                    val verificationResult = VerificationResult.valueOf(verificationResultString)
                    return if (schemaVersion == CUSTOMER_INFO_SCHEMA_VERSION) {
                        CustomerInfoFactory.buildCustomerInfo(
                            cachedJSONObject,
                            requestDate,
                            verificationResult,
                            originalSource,
                            loadedFromCache = true,
                        )
                    } else {
                        null
                    }
                } catch (e: JSONException) {
                    null
                }
            }
    }

    @Synchronized
    public fun cacheCustomerInfo(appUserID: String, info: CustomerInfo) {
        val jsonObject = info.rawData.also {
            it.put(CUSTOMER_INFO_SCHEMA_VERSION_KEY, CUSTOMER_INFO_SCHEMA_VERSION)
            it.put(CUSTOMER_INFO_VERIFICATION_RESULT_KEY, info.entitlements.verification.name)
            it.put(CUSTOMER_INFO_REQUEST_DATE_KEY, info.requestDate.time)
            it.put(CUSTOMER_INFO_ORIGINAL_SOURCE_KEY, info.originalSource.name)
        }
        preferences.edit()
            .putString(
                customerInfoCacheKey(appUserID),
                jsonObject.toString(),
            ).apply()

        setCustomerInfoCacheTimestampToNow(appUserID)
    }

    @Synchronized
    public fun isCustomerInfoCacheStale(appUserID: String, appInBackground: Boolean) =
        getCustomerInfoCachesLastUpdated(appUserID).isCacheStale(appInBackground, dateProvider)

    @Synchronized
    public fun clearCustomerInfoCacheTimestamp(appUserID: String) {
        preferences.edit().clearCustomerInfoCacheTimestamp(appUserID).apply()
    }

    @Synchronized
    public fun clearCustomerInfoCache(appUserID: String) {
        val editor = preferences.edit()
        clearCustomerInfoCache(appUserID, editor)
        editor.apply()
    }

    @Synchronized
    public fun clearCustomerInfoCache(
        appUserID: String,
        editor: SharedPreferences.Editor,
    ) {
        editor.clearCustomerInfoCacheTimestamp(appUserID)
        editor.remove(customerInfoCacheKey(appUserID))
    }

    @Synchronized
    public fun setCustomerInfoCacheTimestampToNow(appUserID: String) {
        setCustomerInfoCacheTimestamp(appUserID, dateProvider.now)
    }

    @Synchronized
    public fun setCustomerInfoCacheTimestamp(appUserID: String, date: Date) {
        preferences.edit().putLong(customerInfoLastUpdatedCacheKey(appUserID), date.time).apply()
    }

    @Synchronized
    public fun setStorefront(countryCode: String) {
        verboseLog { BillingStrings.BILLING_STOREFRONT_CACHING.format(countryCode) }
        preferences.edit().putString(storefrontCacheKey, countryCode).apply()
    }

    @Synchronized
    override fun getStorefront(): String? {
        val storefront = preferences.getString(storefrontCacheKey, null)
        if (storefront == null) {
            debugLog { BillingStrings.BILLING_STOREFRONT_NULL_FROM_CACHE }
        }
        return storefront
    }

    @Synchronized
    private fun getCustomerInfoCachesLastUpdated(appUserID: String): Date {
        return Date(preferences.getLong(customerInfoLastUpdatedCacheKey(appUserID), 0))
    }

    // endregion

    // region virtual currencies
    public fun virtualCurrenciesCacheKey(appUserID: String) = "$virtualCurrenciesCacheBaseKey.$appUserID"

    public fun virtualCurrenciesLastUpdatedCacheKey(appUserID: String) =
        "$virtualCurrenciesLastUpdatedCacheBaseKey.$appUserID"

    @Suppress("SwallowedException", "ForbiddenComment")
    @Synchronized
    public fun getCachedVirtualCurrencies(appUserID: String): VirtualCurrencies? {
        return preferences.getString(virtualCurrenciesCacheKey(appUserID), null)
            ?.let { json ->
                try {
                    return VirtualCurrenciesFactory.buildVirtualCurrencies(jsonString = json)
                } catch (error: JSONException) {
                    log(LogIntent.WARNING) {
                        VirtualCurrencyStrings.ERROR_DECODING_CACHED_VIRTUAL_CURRENCIES.format(error)
                    }
                    null
                } catch (error: SerializationException) {
                    log(LogIntent.WARNING) {
                        VirtualCurrencyStrings.ERROR_DECODING_CACHED_VIRTUAL_CURRENCIES.format(error)
                    }
                    null
                } catch (error: IllegalArgumentException) {
                    log(LogIntent.WARNING) {
                        VirtualCurrencyStrings.ERROR_DECODING_CACHED_VIRTUAL_CURRENCIES.format(error)
                    }
                    null
                }
            }
    }

    @Synchronized
    public fun cacheVirtualCurrencies(appUserID: String, virtualCurrencies: VirtualCurrencies) {
        val virtualCurrenciesJSONString = Json.Default.encodeToString(VirtualCurrencies.serializer(), virtualCurrencies)

        preferences.edit()
            .putString(
                virtualCurrenciesCacheKey(appUserID),
                virtualCurrenciesJSONString,
            ).apply()

        setVirtualCurrenciesCacheTimestampToNow(appUserID)
    }

    @Synchronized
    public fun isVirtualCurrenciesCacheStale(appUserID: String, appInBackground: Boolean) =
        getVirtualCurrenciesCacheLastUpdated(appUserID)
            .isCacheStale(appInBackground, dateProvider)

    @Synchronized
    public fun clearVirtualCurrenciesCache(appUserID: String) {
        val editor = preferences.edit()
        clearVirtualCurrenciesCache(appUserID, editor)
        editor.apply()
    }

    @Synchronized
    public fun clearVirtualCurrenciesCache(
        appUserID: String,
        editor: SharedPreferences.Editor,
    ) {
        editor.clearVirtualCurrenciesCacheTimestamp(appUserID = appUserID)
        editor.clearVirtualCurrenciesCache(appUserID = appUserID)
    }

    @Synchronized
    public fun setVirtualCurrenciesCacheTimestampToNow(appUserID: String) {
        setVirtualCurrenciesCacheTimestamp(appUserID, dateProvider.now)
    }

    @Synchronized
    public fun setVirtualCurrenciesCacheTimestamp(appUserID: String, date: Date) {
        preferences.edit().putLong(virtualCurrenciesLastUpdatedCacheKey(appUserID), date.time).apply()
    }

    @Synchronized
    private fun getVirtualCurrenciesCacheLastUpdated(appUserID: String): Date {
        return Date(preferences.getLong(virtualCurrenciesLastUpdatedCacheKey(appUserID), 0))
    }

    private fun SharedPreferences.Editor.clearVirtualCurrenciesCacheTimestamp(
        appUserID: String,
    ): SharedPreferences.Editor {
        remove(virtualCurrenciesLastUpdatedCacheKey(appUserID))

        getCachedAppUserID()?.let {
            remove(virtualCurrenciesLastUpdatedCacheKey(it))
        }
        getLegacyCachedAppUserID()?.let {
            remove(virtualCurrenciesLastUpdatedCacheKey(it))
        }
        return this
    }

    private fun SharedPreferences.Editor.clearVirtualCurrenciesCache(appUserID: String): SharedPreferences.Editor {
        remove(virtualCurrenciesCacheKey(appUserID))

        getCachedAppUserID()?.let {
            remove(virtualCurrenciesCacheKey(it))
        }
        getLegacyCachedAppUserID()?.let {
            remove(virtualCurrenciesCacheKey(it))
        }
        return this
    }
    // endregion

    // region attribution data

    @Synchronized
    public fun cleanupOldAttributionData() {
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
    public fun getPreviouslySentHashedTokens(): Set<String> {
        return try {
            (preferences.getStringSet(tokensCacheKey, emptySet())?.toSet() ?: emptySet()).also {
                log(LogIntent.DEBUG) { ReceiptStrings.TOKENS_ALREADY_POSTED.format(it) }
            }
        } catch (e: ClassCastException) {
            emptySet()
        }
    }

    @Synchronized
    public fun addSuccessfullyPostedToken(token: String) {
        log(LogIntent.DEBUG) { ReceiptStrings.SAVING_TOKENS_WITH_HASH.format(token, token.sha1()) }
        getPreviouslySentHashedTokens().let {
            log(LogIntent.DEBUG) { ReceiptStrings.TOKENS_IN_CACHE.format(it) }
            setSavedTokenHashes(it.toMutableSet().apply { add(token.sha1()) })
        }
    }

    @Synchronized
    private fun setSavedTokenHashes(newSet: Set<String>) {
        log(LogIntent.DEBUG) { ReceiptStrings.SAVING_TOKENS.format(newSet) }
        preferences.edit().putStringSet(tokensCacheKey, newSet).apply()
    }

    /**
     * Removes from the database all hashed tokens that are not considered active anymore, i.e. all
     * consumed in-apps or inactive subscriptions hashed tokens that are still in the local cache.
     */
    @Synchronized
    public fun cleanPreviouslySentTokens(
        hashedTokens: Set<String>,
    ) {
        log(LogIntent.DEBUG) { ReceiptStrings.CLEANING_PREV_SENT_HASHED_TOKEN }
        setSavedTokenHashes(
            hashedTokens.intersect(getPreviouslySentHashedTokens()),
        )
    }

    /**
     * Returns a list containing all tokens that are in [hashedTokens]
     * that are not present in the device cache.
     * In other words, returns all hashed tokens that are active and have not
     * been posted to our backend yet.
     */
    @Synchronized
    public fun getActivePurchasesNotInCache(
        hashedTokens: Map<String, StoreTransaction>,
    ): List<StoreTransaction> {
        return hashedTokens
            .minus(getPreviouslySentHashedTokens())
            .values.toList()
    }

    // endregion

    // region offerings response

    @Synchronized
    public fun getOfferingsResponseCache(): JSONObject? {
        return getJSONObjectOrNull(offeringsResponseCacheKey)
    }

    @Synchronized
    public fun cacheOfferingsResponse(offeringsResponse: JSONObject) {
        preferences.edit()
            .putString(
                offeringsResponseCacheKey,
                offeringsResponse.toString(),
            ).apply()
    }

    @Synchronized
    public fun clearOfferingsResponseCache() {
        preferences.edit().remove(offeringsResponseCacheKey).apply()
    }

    // endregion

    // region ProductEntitlementMapping

    @Synchronized
    public fun cacheProductEntitlementMapping(productEntitlementMapping: ProductEntitlementMapping) {
        val json = productEntitlementMapping.toJson()
        preferences.edit()
            .putString(
                productEntitlementMappingCacheKey,
                json.toString(),
            )
            .apply()

        setProductEntitlementMappingCacheTimestampToNow()
    }

    @Synchronized
    public fun setProductEntitlementMappingCacheTimestampToNow() {
        setProductEntitlementMappingCacheTimestamp(dateProvider.now)
    }

    private fun setProductEntitlementMappingCacheTimestamp(date: Date) {
        preferences.edit().putLong(productEntitlementMappingLastUpdatedCacheKey, date.time).apply()
    }

    @Synchronized
    public fun isProductEntitlementMappingCacheStale(): Boolean {
        return getProductEntitlementMappingLastUpdated().isCacheStale(
            PRODUCT_ENTITLEMENT_MAPPING_CACHE_REFRESH_PERIOD,
            dateProvider,
        )
    }

    @Suppress("NestedBlockDepth")
    @Synchronized
    public fun getProductEntitlementMapping(): ProductEntitlementMapping? {
        return preferences.getString(productEntitlementMappingCacheKey, null)?.let { jsonString ->
            return try {
                val jsonObject = JSONObject(jsonString)
                ProductEntitlementMapping.fromJson(jsonObject, loadedFromCache = true)
            } catch (e: JSONException) {
                errorLog(e) { OfflineEntitlementsStrings.ERROR_PARSING_PRODUCT_ENTITLEMENT_MAPPING.format(jsonString) }
                preferences.edit()
                    .remove(productEntitlementMappingCacheKey)
                    .apply()
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
        value: String,
    ) {
        preferences.edit().putString(
            cacheKey,
            value,
        ).apply()
    }

    public fun remove(
        cacheKey: String,
    ) {
        preferences.edit().remove(cacheKey).apply()
    }

    public fun findKeysThatStartWith(
        cacheKey: String,
    ): Set<String> {
        return try {
            preferences.all
                ?.filterKeys { it.startsWith(cacheKey) }
                ?.keys ?: emptySet()
        } catch (e: NullPointerException) {
            emptySet()
        }
    }

    public fun newKey(
        key: String,
    ) = "$apiKeyPrefix.$key"

    // endregion
}
