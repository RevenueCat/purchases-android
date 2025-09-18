package com.revenuecat.purchases.storage

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
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
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

/**
 * A file repository that handles downloading and caching files from remote URLs.
 */
@InternalRevenueCatAPI
interface FileRepository {
    /**
     * Prefetch files at the given urls.
     * @param urls An array of URL to fetch data from.
     * @return The Job that is executing the prefetch
     */
    fun prefetch(urls: List<URL>)

    /**
     * Create and/or get the cached file url.
     * @param url The url for the remote data to cache into a file.
     * @return The local file's URI where the data can be found after caching is complete.
     */
    suspend fun generateOrGetCachedFileURL(url: URL): URI

    /**
     * Get the cached file url if it exists.
     * @param url The url for the remote data to cache into a file.
     * @return The local file's URI where the data can be found after caching is complete.
     * */
    fun getFile(url: URL): URI?
}

/**
 * The file repository is a service capable of storing data and returning the URL where that stored data exists.
 */
@InternalRevenueCatAPI
interface LocalFileCache {
    fun generateLocalFilesystemURI(remoteURL: URL): URI?
    fun cachedContentExists(uri: URI): Boolean
    fun saveData(data: ByteArray, uri: URI)
}

@InternalRevenueCatAPI
class DefaultFileRepository
internal constructor(
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val store: KeyedDeferredValueStore<URL, URI> = KeyedDeferredValueStore<URL, URI>(),
    private val fileCacheManager: LocalFileCache,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + NonCancellable),
    private val logHandler: LogHandler = currentLogHandler,
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) : FileRepository {

    // Convenience constructor for Android
    constructor(
        context: Context,
    ) : this(
        fileCacheManager = DefaultFileCache(context),
    )

    override fun prefetch(urls: List<URL>) {
        ioScope.launch {
            urls.forEach { url ->
                try {
                    generateOrGetCachedFileURL(url)
                } catch (e: IOException) {
                    logHandler.e("FileRepository", "Prefetch failed for $url: $e", e)
                }
            }
        }
    }

    override suspend fun generateOrGetCachedFileURL(url: URL): URI {
        return store.getOrPut(url) {
            ioScope.async {
                val cachedUri = fileCacheManager.generateLocalFilesystemURI(remoteURL = url)
                    ?: {
                        val error = Error.FailedToCreateCacheDirectory(url.toString())
                        logHandler.e("FileRepository", "Failed to create cache directory for $url", error)
                        throw error
                    }()

                if (fileCacheManager.cachedContentExists(cachedUri)) {
                    return@async cachedUri
                }

                val data = downloadFile(url = url)
                saveCachedFile(cachedUri, data)
                return@async cachedUri
            }
        }.await()
    }

    override fun getFile(url: URL): URI? =
        fileCacheManager.generateLocalFilesystemURI(remoteURL = url)?.let {
            if (fileCacheManager.cachedContentExists(it)) it else null
        }

    @Suppress("ThrowsCount")
    private suspend fun downloadFile(url: URL): ByteArray {
        return try {
            withContext(Dispatchers.IO) {
                verboseLog { "Downloading remote font from $url" }

                val connection = urlConnectionFactory.createConnection(url.toString())

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP ${connection.responseCode} when downloading file at: $url")
                }
                try {
                    val bytes = connection.inputStream.use { inputStream ->
                        inputStream.readBytes()
                    }
                    connection.disconnect()
                    return@withContext bytes
                } catch (e: IOException) {
                    val message = "Failed to read input stream for file at: $url. Error: ${e.localizedMessage}"
                    logHandler.e("FileRepository", message, e)
                    connection.disconnect()
                    throw Error.FailedToFetchFileFromRemoteSource(message)
                }
            }
        } catch (e: IOException) {
            val message = "Failed to fetch file from remote source: $url. Error: ${e.localizedMessage}"
            logHandler.e("FileRepository", message, e)
            throw Error.FailedToFetchFileFromRemoteSource(message)
        }
    }

    private fun saveCachedFile(uri: URI, data: ByteArray) {
        try {
            fileCacheManager.saveData(data, uri)
        } catch (e: IOException) {
            val message = "Failed to save cached file: $uri. Error: ${e.localizedMessage}"
            logHandler.e("FileRepository", message, e)
            throw Error.FailedToSaveCachedFile(message)
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
    }
}

@InternalRevenueCatAPI
private class DefaultFileCache(
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

    override fun generateLocalFilesystemURI(remoteURL: URL): URI? {
        val urlHash = md5Hex(remoteURL.toString().toByteArray())
        val fileName = File(urlHash).name
        if (fileName.isEmpty()) return null
        return File(cacheDir, fileName).toURI()
    }

    override fun cachedContentExists(uri: URI): Boolean =
        File(uri).exists()

    override fun saveData(data: ByteArray, uri: URI) =
        writeBytes(data, File(uri))

    private fun md5Hex(bytes: ByteArray): String =
        md.digest(bytes).joinToString("") { "%02x".format(it) }

    @Throws(IOException::class)
    private fun writeBytes(data: ByteArray, file: File) {
        FileOutputStream(file).use { out ->
            out.write(data)
        }
    }
}
