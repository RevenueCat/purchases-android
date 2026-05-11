package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.utils.UrlConnectionFactory
import com.revenuecat.purchases.utils.downloadToFileAndVerifyChecksum
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)
internal class TopicFetcher(
    private val applicationContext: Context,
    private val urlConnectionFactory: UrlConnectionFactory,
    private val downloadDispatcher: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(MAX_PARALLEL_TOPIC_DOWNLOADS),
) {
    suspend fun fetchTopicIfNeeded(
        topic: Topic,
        entryId: String,
        topicEntry: TopicEntry,
        source: BlobSource,
    ): PurchasesError? {
        if (!topicEntry.blobRef.matches(BLOB_REF_PATTERN)) {
            val message = "Topic $topic ($entryId) has a malformed blob_ref; refusing to fetch."
            errorLog { message }
            return PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError, message)
        }
        return withContext(downloadDispatcher) { fetchOnDispatcher(topic, entryId, topicEntry, source) }
    }

    private fun fetchOnDispatcher(
        topic: Topic,
        entryId: String,
        topicEntry: TopicEntry,
        source: BlobSource,
    ): PurchasesError? {
        val targetFile = topicFile(topic, topicEntry.blobRef)
        if (targetFile.exists()) {
            verboseLog { "Topic $topic ($entryId) already cached at ${targetFile.absolutePath}" }
            return null
        }
        return try {
            val url = source.urlFormat.replace(BLOB_REF_PLACEHOLDER, topicEntry.blobRef)
            downloadVerifyAndStore(url, topicEntry.blobRef, targetFile)
            debugLog { "Topic $topic ($entryId) downloaded to ${targetFile.absolutePath}" }
            null
        } catch (e: Checksum.ChecksumValidationException) {
            errorLog(e) { "Downloaded topic $topic ($entryId) failed SHA-256 verification." }
            PurchasesError(
                PurchasesErrorCode.NetworkError,
                "Downloaded topic $topic ($entryId) failed SHA-256 verification.",
            )
        } catch (e: IOException) {
            e.toPurchasesError().also { errorLog(it) }
        }
    }

    private fun topicFile(topic: Topic, blobRef: String): File {
        val dir = File(File(applicationContext.noBackupFilesDir, TOPICS_ROOT), topic.key)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, blobRef)
    }

    @Throws(IOException::class, Checksum.ChecksumValidationException::class)
    private fun downloadVerifyAndStore(url: String, expectedSha256: String, target: File) {
        val parent = target.parentFile ?: throw IOException("Topic target file has no parent: $target")
        val tempFile = File.createTempFile("rc_topic_", ".tmp", parent)
        try {
            urlConnectionFactory.downloadToFileAndVerifyChecksum(
                url = url,
                outputFile = tempFile,
                expectedChecksum = Checksum(Checksum.Algorithm.SHA256, expectedSha256),
                description = "topic",
            )
            if (!tempFile.renameTo(target)) {
                try {
                    tempFile.copyTo(target, overwrite = true)
                } catch (e: IOException) {
                    errorLog(e) { "Failed to copy verified topic from temp to target: ${e.message}" }
                    target.delete()
                    throw e
                }
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private companion object {
        const val TOPICS_ROOT = "RevenueCat/topics"
        const val BLOB_REF_PLACEHOLDER = "{blob_ref}"
        const val MAX_PARALLEL_TOPIC_DOWNLOADS = 4
        val BLOB_REF_PATTERN = Regex("^[a-zA-Z0-9]+$")
    }
}
