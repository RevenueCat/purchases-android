package com.revenuecat.purchasetester.ui.screens.proxysettings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchasetester.proxysettings.ProxyMode
import com.revenuecat.purchasetester.proxysettings.ProxySettingsState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ProxySettingsViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    companion object {
        private const val CONNECT_TIMEOUT = 1000
        private const val OK_CODE = 200
    }

    private val _state = MutableStateFlow<ProxySettingsState>(ProxySettingsState.Loading)
    val state: StateFlow<ProxySettingsState> = _state.asStateFlow()

    private var proxyURL = Purchases.proxyURL

    fun loadCurrentState() {
        proxyURL = Purchases.proxyURL
        val statusURL = proxyURL?.let { URL("$it/status") } ?: run {
            updateState { ProxySettingsState.Error("There is no Proxy URL configured") }
            return
        }
        performRequest(statusURL)
    }

    fun changeMode(proxyMode: ProxyMode) {
        val changeModeURL = proxyURL?.let { URL("$it/${proxyMode.requestPath()}") } ?: run {
            updateState { ProxySettingsState.Error("There is no Proxy URL configured") }
            return
        }
        performRequest(changeModeURL)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun performRequest(url: URL) {
        viewModelScope.launch(dispatcher) {
            try {
                Log.w("PurchaseTester", "Performing request to proxy with url: $url")
                val httpURLConnection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = CONNECT_TIMEOUT
                }

                val responseCode = httpURLConnection.responseCode
                if (responseCode != OK_CODE) {
                    error("Invalid response code while executing request to $url: $responseCode")
                }

                httpURLConnection.inputStream.bufferedReader().use { reader ->
                    val responseBody = reader.readText()
                    Log.w("PurchaseTester", "Received response from proxy: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    val mode = ProxyMode.fromString(jsonResponse.getString("mode"))
                    updateState { ProxySettingsState.CurrentMode(mode) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateState { ProxySettingsState.Error(("${e::class.qualifiedName} \n ${e.message}")) }
            }
        }
    }

    private inline fun updateState(
        block: () -> ProxySettingsState,
    ) {
        val newState = block()
        Log.w("PurchaseTester", "New state: $newState")
        _state.value = newState
    }
}
