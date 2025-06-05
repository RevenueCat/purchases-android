package com.revenuecat.purchases.paywalls

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
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
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(InternalRevenueCatAPI::class)
private data class DownloadableFontInfo(
    val url: String,
    val expectedMd5: String,
    val family: String,
    val weight: Int,
    val style: FontStyle,
)

@OptIn(InternalRevenueCatAPI::class)
internal class FontLoader(
    private val context: Context,
    private val cacheDir: File = File(context.cacheDir, "rc_paywall_fonts"),
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) {
    private var hasCheckedFoldersExist: AtomicBoolean = AtomicBoolean(false)

    private val md: MessageDigest by lazy {
        MessageDigest.getInstance("MD5")
    }

    private val fontInfosListeningToSameHashUrl = mutableMapOf<String, MutableSet<DownloadableFontInfo>>()

    private val cachedFontFamilyByFontInfo: MutableMap<DownloadableFontInfo, DownloadedFontFamily> = mutableMapOf()
    private val cachedFontFamilyByFamily: MutableMap<String, DownloadedFontFamily> = mutableMapOf()

    fun getCachedFontFamilyOrStartDownload(fontInfo: FontInfo.Name): DownloadedFontFamily? {
        val fontInfoToDownload = validateFontInfo(fontInfo) ?: return null

        return if (cachedFontFamilyByFontInfo.containsKey(fontInfoToDownload)) {
            cachedFontFamilyByFontInfo[fontInfoToDownload]
        } else {
            startFontDownload(fontInfoToDownload)
            null
        }
    }

    private fun startFontDownload(fontInfo: DownloadableFontInfo) {
        val url = fontInfo.url
        val expectedMd5 = fontInfo.expectedMd5

        ioScope.launch {
            ensureFoldersExist()

            val urlHash = md5Hex(url.toByteArray(Charsets.UTF_8))
            val extension = url.substringAfterLast('.', missingDelimiterValue = "")
            val cachedFile = File(cacheDir, "$urlHash.$extension")

            synchronized(this) {
                val fontInfosListeningToHash = fontInfosListeningToSameHashUrl[urlHash]
                if (fontInfosListeningToHash == null) {
                    fontInfosListeningToSameHashUrl[urlHash] = mutableSetOf(fontInfo)
                } else {
                    verboseLog("Font download already in progress for $url")
                    fontInfosListeningToHash.add(fontInfo)
                    return@launch
                }
            }

            if (cachedFile.exists()) {
                addFileToCache(urlHash, cachedFile)
                return@launch
            }

            try {
                val result = performDownloadAndCache(
                    url = url,
                    expectedMd5 = expectedMd5,
                    urlHash = urlHash,
                    extension = extension,
                )
                result
                    .onSuccess { file ->
                        addFileToCache(urlHash, file)
                    }.onFailure {
                        errorLog("Failed to download font for ${fontInfo.family}")
                    }
            } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
                errorLog("Error downloading remote font from $url", t)
            } finally {
                synchronized(this) {
                    fontInfosListeningToSameHashUrl.remove(urlHash)
                }
            }
        }
    }

    private fun addFileToCache(urlHash: String, file: File) {
        synchronized(this) {
            for (fontInfo in fontInfosListeningToSameHashUrl[urlHash] ?: emptySet()) {
                if (cachedFontFamilyByFontInfo[fontInfo] != null) {
                    verboseLog("Font already cached for ${fontInfo.family}. Skipping download.")
                    continue
                }
                val downloadedFontFamily = cachedFontFamilyByFamily[fontInfo.family]
                if (downloadedFontFamily != null) {
                    downloadedFontFamily.addFont(
                        DownloadedFont(
                            weight = fontInfo.weight,
                            style = fontInfo.style,
                            file = file,
                        ),
                    )
                    cachedFontFamilyByFontInfo[fontInfo] = downloadedFontFamily
                } else {
                    val fontFamily = DownloadedFontFamily(
                        family = fontInfo.family,
                        fonts = mutableListOf(
                            DownloadedFont(
                                weight = fontInfo.weight,
                                style = fontInfo.style,
                                file = file,
                            ),
                        ),
                    )
                    cachedFontFamilyByFontInfo[fontInfo] = fontFamily
                    cachedFontFamilyByFamily[fontInfo.family] = fontFamily
                }
            }
            fontInfosListeningToSameHashUrl.remove(urlHash)
        }
    }

    @Suppress("ReturnCount")
    private fun validateFontInfo(fontInfo: FontInfo.Name): DownloadableFontInfo? {
        if (fontInfo.url.isNullOrEmpty()) {
            errorLog("Font URL is empty for ${fontInfo.value}. Cannot download font.")
            return null
        }
        if (fontInfo.hash.isNullOrEmpty()) {
            errorLog("Font hash is empty for ${fontInfo.value}. Cannot validate downloaded font.")
            return null
        }
        if (fontInfo.family.isNullOrEmpty()) {
            errorLog("Font family is empty for ${fontInfo.value}. Cannot download font.")
            return null
        }
        if (fontInfo.weight == null) {
            errorLog("Font weight is null for ${fontInfo.value}.")
            return null
        }
        if (fontInfo.style == null) {
            errorLog("Font style is empty for ${fontInfo.value}.")
            return null
        }
        return DownloadableFontInfo(
            url = fontInfo.url,
            expectedMd5 = fontInfo.hash,
            family = fontInfo.family,
            weight = fontInfo.weight,
            style = fontInfo.style,
        )
    }

    private fun ensureFoldersExist() {
        if (hasCheckedFoldersExist.getAndSet(true)) return

        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            errorLog("Unable to create cache directory for remote fonts: ${cacheDir.absolutePath}")
        } else if (!cacheDir.isDirectory) {
            errorLog("Remote fonts cache path exists but is not a directory: ${cacheDir.absolutePath}")
        }
    }

    @Throws(IOException::class)
    @Suppress("ReturnCount")
    private fun performDownloadAndCache(
        url: String,
        expectedMd5: String,
        urlHash: String,
        extension: String,
    ): Result<File> {
        val cachedFile = File(cacheDir, "$urlHash.$extension")

        val tempFile = File.createTempFile("rc_paywall_font_download_", ".$extension", cacheDir)
        try {
            downloadToFile(url, tempFile)

            val actualMd5 = md5Hex(tempFile.readBytes())
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                tempFile.delete()
                errorLog("Downloaded font file is corrupt for $url. expected=$expectedMd5, actual=$actualMd5")
                return Result.failure(IOException("Downloaded font file is corrupt for $url"))
            }

            if (!tempFile.renameTo(cachedFile)) {
                tempFile.copyTo(cachedFile, overwrite = true)
                tempFile.delete()
            }
            debugLog("Font downloaded successfully from $url")
            return Result.success(cachedFile)
        } catch (e: IOException) {
            if (tempFile.exists()) tempFile.delete()
            errorLog("Error downloading font from $url: ${e.message}")
            return Result.failure(e)
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
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
