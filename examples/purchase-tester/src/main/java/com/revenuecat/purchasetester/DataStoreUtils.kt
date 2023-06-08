package com.revenuecat.purchasetester

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.configurationDataStore: DataStore<Preferences> by preferencesDataStore(name = "configuration")

class DataStoreUtils(
    private val dataStore: DataStore<Preferences>,
) {
    private val apiKeyKey = stringPreferencesKey("last_sdk_api_key")
    private val proxyUrlKey = stringPreferencesKey("last_proxy_url_key")
    private val useAmazonKey = booleanPreferencesKey("last_use_amazon_key")

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
            preferences[useAmazonKey] = sdkConfiguration.useAmazon
        }
    }

    fun getSdkConfig(): Flow<SdkConfiguration> {
        return dataStore.data.map { preferences ->
            SdkConfiguration(
                apiKey = preferences[apiKeyKey] ?: "",
                proxyUrl = preferences[proxyUrlKey],
                useAmazon = preferences[useAmazonKey] ?: false,
            )
        }
    }
}
