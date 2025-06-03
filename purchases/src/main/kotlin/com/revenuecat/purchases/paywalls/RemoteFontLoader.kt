package com.revenuecat.purchases.paywalls

import android.content.Context
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.security.MessageDigest

internal class RemoteFontLoader(
    private val context: Context,
    private val cacheDir: File = File(context.cacheDir, "rc_paywall_fonts"),
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) {

    private val ongoingDownloads = mutableSetOf<String>()

    init {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                errorLog("Unable to create cache directory for remote fonts: ${cacheDir.absolutePath}")
            }
        } else if (!cacheDir.isDirectory) {
            errorLog("Remote fonts cache path exists but is not a directory: ${cacheDir.absolutePath}")
        }
    }

    fun getCachedFontFileOrStartDownload(url: String, expectedMd5: String): File? {
        val urlHash = md5Hex(url.toByteArray(Charsets.UTF_8))
        val extension = url.substringAfterLast('.')
        val cachedFile = File(cacheDir, "$urlHash.$extension")

        if (cachedFile.exists()) {
            return cachedFile
        }

        ioScope.launch {
            synchronized(this) {
                if (ongoingDownloads.contains(urlHash)) {
                    verboseLog("Font download already in progress for $url")
                    return@launch
                }
                ongoingDownloads.remove(urlHash)
            }

            try {
                performDownloadAndCache(
                    url = url,
                    expectedMd5 = expectedMd5,
                    urlHash = urlHash,
                    extension = extension,
                )
            } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
                errorLog("Error downloading remote font from $url: ${t.message}")
            } finally {
                synchronized(this) {
                    ongoingDownloads.remove(urlHash)
                }
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun performDownloadAndCache(
        url: String,
        expectedMd5: String,
        urlHash: String,
        extension: String,
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
        verboseLog("Downloading remote font from $url")
        var connection: UrlConnection? = null
        try {
            connection = urlConnectionFactory.createConnection(url)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode} when downloading paywall font: $url")
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
            var bytesRead: Int
            while (true) {
                bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                out.write(buffer, 0, bytesRead)
            }
        }
    }

    private fun md5Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
