@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.storage.FileRepository
import java.io.File
import java.io.IOException
import java.net.URL

/**
 * Fetches compiled workflow JSON from a CDN URL (inject for tests; use [FileCachedWorkflowCdnFetcher] in production).
 */
internal fun interface WorkflowCdnFetcher {
    @Throws(IOException::class)
    suspend fun fetchCompiledWorkflowJson(cdnUrl: String, checksum: Checksum?): String
}

/**
 * Uses [FileRepository] for disk caching and checksum validation.
 */
internal class FileCachedWorkflowCdnFetcher(
    private val fileRepository: FileRepository,
) : WorkflowCdnFetcher {

    @Throws(IOException::class)
    override suspend fun fetchCompiledWorkflowJson(cdnUrl: String, checksum: Checksum?): String {
        val url = URL(cdnUrl)
        val uri = fileRepository.generateOrGetCachedFileURL(url, checksum)
        return File(uri).readText()
    }
}
