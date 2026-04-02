package com.revenuecat.purchases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.purchases_core.HttpClient
import uniffi.purchases_core.HttpException
import java.net.URL

/**
 * PoC: Kotlin implementation of the Rust `HttpClient` foreign trait.
 * Rust calls `fetch(url)` on this object and awaits the result.
 */
internal class NativeHttpClient : HttpClient {

    override suspend fun fetch(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                URL(url).readText()
            } catch (e: Exception) {
                throw HttpException.RequestFailed("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

}
