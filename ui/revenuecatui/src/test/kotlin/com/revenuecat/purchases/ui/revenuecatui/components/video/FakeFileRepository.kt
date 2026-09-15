package com.revenuecat.purchases.ui.revenuecatui.components.video

import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.storage.FileRepository
import java.net.URI
import java.net.URL

internal class FakeFileRepository(
    private val cachedFiles: Map<URL, URI> = emptyMap(),
    private val failingUrls: Set<URL> = emptySet(),
) : FileRepository {

    val getFileRequests: MutableList<URL> = mutableListOf()
    val generateRequests: MutableList<URL> = mutableListOf()

    override fun prefetch(urls: List<Pair<URL, Checksum?>>) = Unit

    override fun getFile(url: URL, checksum: Checksum?): URI? {
        getFileRequests += url
        return cachedFiles[url]
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum?): URI {
        generateRequests += url
        if (url in failingUrls) {
            throw RuntimeException("Simulated failure for $url")
        }
        return cachedFiles[url] ?: throw RuntimeException("Missing fake URI for $url")
    }
}
