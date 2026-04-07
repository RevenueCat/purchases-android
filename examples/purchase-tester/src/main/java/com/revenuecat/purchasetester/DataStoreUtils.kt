package com.revenuecat.purchasetester

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.revenuecat.purchases_sample.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.configurationDataStore: DataStore<Preferences> by preferencesDataStore(name = "configuration")

class DataStoreUtils(
    private val dataStore: DataStore<Preferences>,
) {
    private val apiKeyKey = stringPreferencesKey("last_sdk_api_key")
    private val proxyUrlKey = stringPreferencesKey("last_proxy_url_key")
    private val useAmazonKey = booleanPreferencesKey("last_use_amazon_key")
    private val storeKey = stringPreferencesKey("last_store_key")
    private val appUserIdKey = stringPreferencesKey("last_app_user_id_key")

    suspend fun saveSdkConfig(
        sdkConfiguration: SdkConfiguration,
    ) {
        dataStore.edit { preferences ->
            preferences[apiKeyKey] = sdkConfiguration.apiKey
            if (sdkConfiguration.proxyUrl == null) {
                preferences.remove(proxyUrlKey)
            } else {
                preferences[proxyUrlKey] = sdkConfiguration.proxyUrl
            }
            preferences[useAmazonKey] = sdkConfiguration.store == Store.AMAZON
            preferences[storeKey] = sdkConfiguration.store.name
            if (sdkConfiguration.appUserId == null) {
                preferences.remove(appUserIdKey)
            } else {
                preferences[appUserIdKey] = sdkConfiguration.appUserId
            }
        }
    }

    fun getSdkConfig(): Flow<SdkConfiguration> {
        return dataStore.data.map { preferences ->
            val storedStore = Store.fromName(preferences[storeKey])
            val fallbackStore = if (preferences[useAmazonKey] == true) Store.AMAZON else Store.GOOGLE
            SdkConfiguration(
                apiKey = preferences[apiKeyKey] ?: BuildConfig.REVENUECAT_API_KEY,
                proxyUrl = preferences[proxyUrlKey],
                store = storedStore ?: fallbackStore,
                appUserId = preferences[appUserIdKey],
            )
        }
    }
}
