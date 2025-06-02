package com.revenuecat.purchases.paywalls

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture

@RequiresApi(Build.VERSION_CODES.N)
internal class RemoteFontLoader(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val cacheDir: File = File(context.cacheDir, "rc_paywall_fonts"),
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + backgroundDispatcher),
) {

    private val ongoingDownloads = mutableMapOf<String, CompletableFuture<File>>()

    init {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                errorLog("Unable to create cache directory for remote fonts: ${cacheDir.absolutePath}")
            }
        } else if (!cacheDir.isDirectory) {
            errorLog("Remote fonts cache path exists but is not a directory: ${cacheDir.absolutePath}")
        }
    }

    fun getOrDownloadFont(url: String, expectedMd5: String): CompletableFuture<File> {
        val urlHash = md5Hex(url.toByteArray(Charsets.UTF_8))
        val extension = url.substringAfterLast('.', missingDelimiterValue = "bin")
        val cachedFile = File(cacheDir, "$urlHash.$extension")
        val resultFuture = CompletableFuture<File>()

        ioScope.launch {
            try {
                if (cachedFile.exists()) {
                    resultFuture.complete(cachedFile)
                    return@launch
                }

                val ongoing: CompletableFuture<File> = synchronized(this) {
                    ongoingDownloads[urlHash]?.let { return@synchronized it }

                    val newFuture = CompletableFuture<File>()
                    ongoingDownloads[urlHash] = newFuture
                    newFuture
                }

                if (!ongoing.isDone && !ongoing.isCancelled) {
                    try {
                        val downloadedFile = performDownloadAndCache(
                            url = url,
                            expectedMd5 = expectedMd5,
                            urlHash = urlHash,
                            extension = extension
                        )
                        ongoing.complete(downloadedFile)
                    } catch (t: Throwable) {
                        ongoing.completeExceptionally(t)
                    } finally {
                        synchronized(this) {
                            ongoingDownloads.remove(urlHash)
                        }
                    }
                }

                try {
                    val file = ongoing.get() // blocks inside IO thread, but not on caller thread
                    resultFuture.complete(file)
                } catch (e: Exception) {
                    resultFuture.completeExceptionally(e.cause ?: e)
                }
            } catch (exception: Throwable) {
                resultFuture.completeExceptionally(exception)
            }
        }

        return resultFuture
    }

    @Throws(IOException::class)
    private fun performDownloadAndCache(
        url: String,
        expectedMd5: String,
        urlHash: String,
        extension: String
    ): File {
        val cachedFile = File(cacheDir, "$urlHash.$extension")

        val tempFile = File.createTempFile("rc_paywall_font_download_", ".$extension", cacheDir)
        try {
            downloadToFile(url, tempFile)

            val actualMd5 = md5Hex(tempFile.readBytes())
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                tempFile.delete()
                errorLog("Font download MD5 mismatch for $url: expected=$expectedMd5, actual=$actualMd5")
                throw IOException("MD5 mismatch for $url: expected=$expectedMd5, actual=$actualMd5")
            }

            if (!tempFile.renameTo(cachedFile)) {
                tempFile.copyTo(cachedFile, overwrite = true)
                tempFile.delete()
            }
            debugLog("Font downloaded successfully and cached: $url")
            return cachedFile
        } catch (e: IOException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    @Throws(IOException::class)
    private fun downloadToFile(url: String, outputFile: File) {
        val request = Request.Builder().url(url).build()
        verboseLog("Downloading remote font from $url")
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} when downloading paywall font: $url")
            }
            val bodyStream = response.body?.byteStream()
                ?: throw IOException("Empty response body downloading paywall font: $url")
            writeStream(bodyStream, outputFile)
        }
    }

    /**
     * Simple buffered copy from [input] to [file].
     */
    @Throws(IOException::class)
    private fun writeStream(input: InputStream, file: File) {
        FileOutputStream(file).use { out ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            while (true) {
                bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                out.write(buffer, 0, bytesRead)
            }
        }
    }

    /**
     * Computes the MD5 digest of [bytes] and returns it as a lowercase hex string.
     */
    private fun md5Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
