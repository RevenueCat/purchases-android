package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

@OptIn(InternalRevenueCatAPI::class)
internal class TopicFetcher(
    private val applicationContext: Context,
    private val urlConnectionFactory: UrlConnectionFactory,
    private val dispatcher: Dispatcher,
) {
    fun fetchTopicIfNeeded(
        topic: Topic,
        variant: String,
        topicEntry: TopicEntry,
        source: Source,
        completion: (PurchasesError?) -> Unit,
    ) {
        val targetFile = topicFile(topic, topicEntry.blobRef)
        if (targetFile.exists()) {
            verboseLog { "Topic $topic ($variant) already cached at ${targetFile.absolutePath}" }
            completion(null)
            return
        }
        dispatcher.enqueue(
            Runnable {
                try {
                    val url = source.urlFormat.replace(BLOB_REF_PLACEHOLDER, topicEntry.blobRef)
                    downloadVerifyAndStore(url, topicEntry.blobRef, targetFile)
                    debugLog { "Topic $topic ($variant) downloaded to ${targetFile.absolutePath}" }
                    completion(null)
                } catch (e: Checksum.ChecksumValidationException) {
                    errorLog(e) { "Downloaded topic $topic ($variant) failed SHA-256 verification." }
                    completion(
                        PurchasesError(
                            PurchasesErrorCode.NetworkError,
                            "Downloaded topic $topic ($variant) failed SHA-256 verification.",
                        ),
                    )
                } catch (e: IOException) {
                    completion(e.toPurchasesError().also { errorLog(it) })
                }
            },
        )
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
            downloadToFile(url, tempFile)
            val actual = Checksum.generate(tempFile.readBytes(), Checksum.Algorithm.SHA256).value
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                errorLog {
                    "Downloaded topic at $url failed SHA-256 verification. " +
                        "expected=$expectedSha256, actual=$actual"
                }
                throw Checksum.ChecksumValidationException()
            }
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    @Throws(IOException::class)
    private fun downloadToFile(url: String, outputFile: File) {
        verboseLog { "Downloading topic from $url" }
        var connection: UrlConnection? = null
        try {
            connection = urlConnectionFactory.createConnection(url)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode} when downloading topic from $url")
            }
            connection.inputStream.use { input ->
                writeStream(input, outputFile)
            }
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun writeStream(input: InputStream, file: File) {
        FileOutputStream(file).use { out ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                out.write(buffer, 0, bytesRead)
            }
        }
    }

    private companion object {
        const val TOPICS_ROOT = "RevenueCat/topics"
        const val BLOB_REF_PLACEHOLDER = "{blob_ref}"
    }
}
