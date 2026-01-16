package com.revenuecat.purchases.paywalls

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.fonts.DownloadableFontInfo
import com.revenuecat.purchases.paywalls.fonts.toDownloadableFontInfo
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
import com.revenuecat.purchases.utils.Result as RCResult

@OptIn(InternalRevenueCatAPI::class)
internal class FontLoader(
    private val context: Context,
    private val providedCacheDir: File? = null,
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) {
    private var hasCheckedFoldersExist: AtomicBoolean = AtomicBoolean(false)

    private val cacheDirectory: File? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        providedCacheDir ?: context.cacheDir?.let { File(it, "rc_paywall_fonts") }
    }

    private val md: MessageDigest by lazy {
        MessageDigest.getInstance("MD5")
    }

    private val fontInfosForHash = mutableMapOf<String, MutableSet<DownloadableFontInfo>>()
    private val lock = Any()

    private val cachedFontFamilyByFontInfo: MutableMap<DownloadableFontInfo, String> = mutableMapOf()
    private val cachedFontFamilyByFamilyName: MutableMap<String, DownloadedFontFamily> = mutableMapOf()

    @Suppress("ReturnCount")
    fun getCachedFontFamilyOrStartDownload(fontInfo: FontInfo.Name): DownloadedFontFamily? {
        val fontInfoToDownload = when (val downloadableFontInfoResult = fontInfo.toDownloadableFontInfo()) {
            is RCResult.Success -> downloadableFontInfoResult.value
            is RCResult.Error -> {
                errorLog { downloadableFontInfoResult.value }
                return null
            }
        }

        synchronized(lock) {
            val cachedFontFamilyName = cachedFontFamilyByFontInfo[fontInfoToDownload]
            val cachedFontFamily = cachedFontFamilyByFamilyName[cachedFontFamilyName]
            if (cachedFontFamily != null) {
                return cachedFontFamily
            }
        }

        startFontDownload(fontInfoToDownload)
        return null
    }

    private fun startFontDownload(fontInfo: DownloadableFontInfo) {
        val url = fontInfo.url
        val expectedMd5 = fontInfo.expectedMd5

        ioScope.launch {
            val cacheDir = cacheDirectory
            if (cacheDir == null) {
                errorLog { "Cannot download font: cache directory is not available" }
                return@launch
            }
            if (!ensureFoldersExist(cacheDir)) {
                return@launch
            }

            val urlHash = md5Hex(url.toByteArray(Charsets.UTF_8))
            val extension = url.substringAfterLast('.', missingDelimiterValue = "")
            val cachedFile = File(cacheDir, "$urlHash.$extension")

            synchronized(lock) {
                val fontInfosListeningToHash = fontInfosForHash[urlHash]
                if (fontInfosListeningToHash == null) {
                    fontInfosForHash[urlHash] = mutableSetOf(fontInfo)
                } else {
                    verboseLog { "Font download already in progress for $url" }
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
                    cacheDir = cacheDir,
                )
                result
                    .onSuccess { file ->
                        addFileToCache(urlHash, file)
                    }.onFailure {
                        errorLog { "Failed to download font for ${fontInfo.family}" }
                    }
            } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
                errorLog(t) { "Error downloading remote font from $url" }
            } finally {
                synchronized(lock) {
                    fontInfosForHash.remove(urlHash)
                }
            }
        }
    }

    private fun addFileToCache(urlHash: String, file: File) {
        synchronized(lock) {
            for (fontInfo in fontInfosForHash[urlHash] ?: emptySet()) {
                val familyName = fontInfo.family
                if (cachedFontFamilyByFontInfo[fontInfo] != null) {
                    verboseLog { "Font already cached for $familyName. Skipping download." }
                    continue
                }
                val downloadedFontFamily = cachedFontFamilyByFamilyName[familyName]
                if (downloadedFontFamily != null) {
                    val newDownloadedFontFamily = DownloadedFontFamily(
                        family = downloadedFontFamily.family,
                        fonts = downloadedFontFamily.fonts + DownloadedFont(
                            weight = fontInfo.weight,
                            style = fontInfo.style,
                            file = file,
                        ),
                    )
                    cachedFontFamilyByFamilyName[familyName] = newDownloadedFontFamily
                    cachedFontFamilyByFontInfo[fontInfo] = familyName
                } else {
                    val fontFamily = DownloadedFontFamily(
                        family = familyName,
                        fonts = listOf(
                            DownloadedFont(
                                weight = fontInfo.weight,
                                style = fontInfo.style,
                                file = file,
                            ),
                        ),
                    )
                    cachedFontFamilyByFontInfo[fontInfo] = familyName
                    cachedFontFamilyByFamilyName[familyName] = fontFamily
                }
            }
            fontInfosForHash.remove(urlHash)
        }
    }

    private fun ensureFoldersExist(cacheDir: File): Boolean {
        if (hasCheckedFoldersExist.get()) return true

        val success = when {
            !cacheDir.exists() && !cacheDir.mkdirs() -> {
                errorLog { "Unable to create cache directory for remote fonts: ${cacheDir.absolutePath}" }
                false
            }
            !cacheDir.isDirectory -> {
                errorLog { "Remote fonts cache path exists but is not a directory: ${cacheDir.absolutePath}" }
                false
            }
            else -> true
        }
        hasCheckedFoldersExist.set(success)
        return success
    }

    @Throws(IOException::class)
    @Suppress("ReturnCount")
    private fun performDownloadAndCache(
        url: String,
        expectedMd5: String,
        urlHash: String,
        extension: String,
        cacheDir: File,
    ): Result<File> {
        val cachedFile = File(cacheDir, "$urlHash.$extension")

        val tempFile = File.createTempFile("rc_paywall_font_download_", ".$extension", cacheDir)
        try {
            downloadToFile(url, tempFile)

            val actualMd5 = md5Hex(tempFile.readBytes())
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                tempFile.delete()
                errorLog { "Downloaded font file is corrupt for $url. expected=$expectedMd5, actual=$actualMd5" }
                return Result.failure(IOException("Downloaded font file is corrupt for $url"))
            }

            if (!tempFile.renameTo(cachedFile)) {
                tempFile.copyTo(cachedFile, overwrite = true)
                tempFile.delete()
            }
            debugLog { "Font downloaded successfully from $url" }
            return Result.success(cachedFile)
        } catch (e: IOException) {
            if (tempFile.exists()) tempFile.delete()
            errorLog { "Error downloading font from $url: ${e.message}" }
            return Result.failure(e)
        }
    }

    @Throws(IOException::class)
    private fun downloadToFile(url: String, outputFile: File) {
        verboseLog { "Downloading remote font from $url" }
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
