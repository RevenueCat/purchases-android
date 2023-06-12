package com.revenuecat.purchasetester.proxysettings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Purchases
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProxySettingsBottomSheetViewModel(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
) : ViewModel() {

    companion object {
        private const val CONNECT_TIMEOUT = 1000
        private const val OK_CODE = 200
    }

    private val _state: MutableLiveData<ProxySettingsState> = MutableLiveData(ProxySettingsState.Loading)
    val state: LiveData<ProxySettingsState> = _state

    private val proxyURL = Purchases.proxyURL

    init {
        queryCurrentState()
    }

    fun changeMode(proxyMode: ProxyMode) {
        val changeModeURL = proxyURL?.let { URL("$it/${proxyMode.requestPath()}") } ?: run {
            updateState(ProxySettingsState.Error("There is no Proxy URL configured"))
            return
        }
        performRequest(changeModeURL)
    }

    private fun queryCurrentState() {
        val statusURL = proxyURL?.let { URL("$it/status") } ?: run {
            updateState(ProxySettingsState.Error("There is no Proxy URL configured"))
            return
        }
        performRequest(statusURL)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun performRequest(url: URL) {
        executor.execute {
            try {
                Log.w("PurchaseTester", "Performing request to proxy with url: $url")
                val httpURLConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = CONNECT_TIMEOUT
                val inputStream = httpURLConnection.inputStream
                val responseCode = httpURLConnection.responseCode
                if (responseCode != OK_CODE) {
                    error("Invalid response code while executing request to $url: $responseCode")
                }
                val responseBody = BufferedReader(InputStreamReader(inputStream)).readText()
                Log.w("PurchaseTester", "Received response from proxy: $responseBody")
                val jsonResponse = JSONObject(responseBody)
                val mode = ProxyMode.fromString(jsonResponse.getString("mode"))
                updateState(ProxySettingsState.CurrentMode(mode))
            } catch (e: Exception) {
                updateState(ProxySettingsState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun updateState(newState: ProxySettingsState) {
        Log.w("PurchaseTester", "New state: $newState")
        _state.postValue(newState)
    }
}
