package com.revenuecat.purchases.storage

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.models.toHexString
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

/**
 * A file repository that handles downloading and caching files from remote URLs.
 */
@InternalRevenueCatAPI
public interface FileRepository {
    /**
     * Prefetch files at the given urls.
     * @param urls An array of the pairs of URL to their checksum to fetch data from.
     * @param checksums Optional checksums for each URL (must match length of urls if provided)
     * @return The Job that is executing the prefetch
     */
    public fun prefetch(urls: List<Pair<URL, Checksum?>>)

    /**
     * Create and/or get the cached file url.
     * @param url The url for the remote data to cache into a file.
     * @param checksum Optional checksum to validate the downloaded file
     * @return The local file's URI where the data can be found after caching is complete.
     */
    public suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum? = null): URI

    /**
     * Get the cached file url if it exists.
     * @param url The url for the remote data to cache into a file.
     * @param checksum Optional checksum (files cached with different checksums are separate)
     * @return The local file's URI where the data can be found after caching is complete.
     * */
    public fun getFile(url: URL, checksum: Checksum? = null): URI?
}

/**
 * The file repository is a service capable of storing data and returning the URL where that stored data exists.
 */
@InternalRevenueCatAPI
public interface LocalFileCache {
    public fun generateLocalFilesystemURI(remoteURL: URL, checksum: Checksum? = null): URI?
    public fun cachedContentExists(uri: URI): Boolean
    public fun saveData(inputStream: InputStream, uri: URI, checksum: Checksum? = null)
}

@InternalRevenueCatAPI
internal class DefaultFileRepository(
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val store: KeyedDeferredValueStore<CacheKey, URI> = KeyedDeferredValueStore(),
    private val fileCacheManager: LocalFileCache,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + NonCancellable),
    private val logHandler: LogHandler = currentLogHandler,
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) : FileRepository {

    internal data class CacheKey(
        public val url: URL,
        public val checksum: Checksum?,
    )

    constructor(
        context: Context,
    ) : this(
        fileCacheManager = DefaultFileCache(context),
    )

    override fun prefetch(urls: List<Pair<URL, Checksum?>>) {
        ioScope.launch {
            urls.forEach { (url, checksum) ->
                try {
                    generateOrGetCachedFileURL(url, checksum)
                } catch (e: IOException) {
                    logHandler.e("FileRepository", "Prefetch failed for $url: $e", e)
                }
            }
        }
    }

    override suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum?): URI {
        return store.getOrPut(CacheKey(url, checksum)) {
            ioScope.async {
                val cachedUri = fileCacheManager.generateLocalFilesystemURI(
                    remoteURL = url,
                    checksum = checksum,
                ) ?: {
                    val error = Error.FailedToCreateCacheDirectory(url.toString())
                    logHandler.e("FileRepository", "Failed to create cache directory for $url", error)
                    throw error
                }()

                if (fileCacheManager.cachedContentExists(cachedUri)) {
                    return@async cachedUri
                }

                val connectionWithStream = downloadFile(url = url)
                saveCachedFile(cachedUri, connectionWithStream, checksum)
                return@async cachedUri
            }
        }.await()
    }

    override fun getFile(url: URL, checksum: Checksum?): URI? =
        fileCacheManager.generateLocalFilesystemURI(remoteURL = url, checksum = checksum)?.let {
            if (fileCacheManager.cachedContentExists(it)) it else null
        }

    private suspend fun downloadFile(url: URL): UrlConnection = try {
        withContext(Dispatchers.IO) {
            verboseLog { "Downloading remote file from $url" }

            val connection = urlConnectionFactory.createConnection(url.toString())

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("HTTP ${connection.responseCode} when downloading file at: $url")
            }

            return@withContext connection
        }
    } catch (e: IOException) {
        val message = "Failed to fetch file from remote source: $url. Error: ${e.localizedMessage}"
        logHandler.e("FileRepository", message, e)
        throw Error.FailedToFetchFileFromRemoteSource(message)
    }

    private fun saveCachedFile(uri: URI, connectionWithStream: UrlConnection, checksum: Checksum?) {
        try {
            connectionWithStream.inputStream.use { stream ->
                fileCacheManager.saveData(stream, uri, checksum)
            }
        } catch (e: Checksum.ChecksumValidationException) {
            val message = "Checksum validation failed for $uri: ${e.message}"
            logHandler.e("FileRepository", message, e)
            throw Error.ChecksumValidationFailed(message)
        } catch (e: IOException) {
            val message = "Failed to save cached file: $uri. Error: ${e.localizedMessage}"
            logHandler.e("FileRepository", message, e)
            throw Error.FailedToSaveCachedFile(message)
        } finally {
            connectionWithStream.disconnect()
        }
    }

    /**
     * File repository error cases.
     */
    sealed class Error(message: String) : IOException(message) {
        /**
         * Used when creating the folder on disk fails.
         */
        class FailedToCreateCacheDirectory(url: String) : Error("Failed to create cache directory for $url")

        /**
         * Used when saving the file on disk fails.
         */
        class FailedToSaveCachedFile(message: String) : Error(message)

        /**
         * Used when fetching the data fails.
         */
        class FailedToFetchFileFromRemoteSource(message: String) : Error(message)

        /**
         * Used when checksum validation fails.
         */
        class ChecksumValidationFailed(message: String) : Error(message)
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal class DefaultFileCache(
    private val context: Context,
) : LocalFileCache {

    private val md: MessageDigest by lazy {
        MessageDigest.getInstance("MD5")
    }

    private val cacheDir: File by lazy {
        val dir = File(context.cacheDir, "rc_files")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    override fun generateLocalFilesystemURI(remoteURL: URL, checksum: Checksum?): URI? {
        val urlHash = md5Hex(remoteURL.toString().toByteArray())
        // Use checksum value as part of the file name (like iOS)
        val fileName = File(urlHash).name + (checksum?.value ?: "")
        if (fileName.isEmpty()) return null

        val extension = remoteURL.path.substringAfterLast('.', "")
        val fileWithExtension = "$fileName.$extension"

        return File(cacheDir, fileWithExtension).toURI()
    }

    override fun cachedContentExists(uri: URI): Boolean =
        File(uri).exists()

    // For readability. I reads like a sentence and ! is harder to see than isFalse
    private val Boolean.isFalse: Boolean get() = !this

    override fun saveData(inputStream: InputStream, uri: URI, checksum: Checksum?) {
        val finalFile = File(uri)
        val tempFile = File.createTempFile(
            "rc_download_",
            ".tmp",
            finalFile.parentFile,
        )

        try {
            // Stream to temp file, optionally calculating checksum if available
            if (checksum != null) {
                if (streamToFileAndCompareChecksum(inputStream, tempFile, checksum).isFalse) return
            } else {
                streamToFile(inputStream, tempFile)
            }

            // We will first attempt to just move the file in one shot. This is an atomic and very fast operation
            // but it is prone to failure if the temp directory and cache directory are on different volumes
            // like the SDCard and the device's internal storage
            if (!tempFile.renameTo(finalFile)) {
                // If that rename fails, we will try again with a slower,
                // non-atomic, operation that will work across volumes
                @Suppress("TooGenericExceptionCaught")
                try {
                    tempFile.copyTo(finalFile, overwrite = true)
                } catch (e: Exception) {
                    verboseLog { "Failed to copy temp file to final file: ${e.message}" }
                    finalFile.delete()
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun md5Hex(bytes: ByteArray): String =
        md.digest(bytes).joinToString("") { "%02x".format(it) }

    @Throws(IOException::class)
    private fun streamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream, bufferSize = BUFFER_SIZE)
        }
    }

    @Throws(IOException::class)
    private fun streamToFileAndCompareChecksum(
        inputStream: InputStream,
        file: File,
        checksum: Checksum,
    ): Boolean {
        val digest = MessageDigest.getInstance(checksum.algorithm.algorithmName)

        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Update checksum
                digest.update(buffer, 0, bytesRead)

                // Write to file
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
        }

        val hash = digest.digest()

        val computedChecksum = Checksum(
            checksum.algorithm,
            hash.toHexString(),
        )

        return checksum == computedChecksum
    }

    public companion object {
        private const val BUFFER_SIZE = 256 * 1024 // 256KB
    }
}
