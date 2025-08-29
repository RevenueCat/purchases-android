package com.revenuecat.purchases.storage

import android.content.Context
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

/**
 * A file cache.
 */
internal interface FileRepositoryType {
    /**
     * Prefetch files at the given urls.
     * @param urls An array of URL to fetch data from.
     * @return The Job that is executing the prefetch
     */
    fun prefetch(urls: List<URL>): Job

    /**
     * Create and/or get the cached file url.
     * @param url The url for the remote data to cache into a file.
     * @return The local file's URI where the data can be found after caching is complete.
     */
    suspend fun generateOrGetCachedFileURL(url: URL): URI
}

/**
 * The file manager is a service capable of storing data and returning the URL where that stored data exists.
 */
internal interface LargeItemCacheType {
    fun generateLocalFilesystemURI(remoteURL: URL): URI?
    fun cachedContentExists(uri: URI): Boolean
    fun saveData(data: InputStream, uri: URI)
}

internal class FileRepository(
    private val fileCacheManager: LargeItemCacheType,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val logHandler: LogHandler = currentLogHandler,
    private val urlConnectionFactory: UrlConnectionFactory = DefaultUrlConnectionFactory(),
) : FileRepositoryType {

    // Convenience constructor for Android
    constructor(
        context: Context,
    ) : this(
        fileCacheManager = FileCache(context),
    )

    private val store = KeyedDeferredValueStore<URL, URI>()

    override fun prefetch(urls: List<URL>) = scope.launch {
        urls.forEach { url ->
            try {
                generateOrGetCachedFileURL(url)
            } catch (e: IOException) {
                logHandler.e("FileRepository", "Prefetch failed for $url: $e", e)
            }
        }
    }

    override suspend fun generateOrGetCachedFileURL(url: URL): URI {
        return store.getOrPut(url) {
            scope.async {
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

    private suspend fun downloadFile(url: URL): InputStream {
        return try {
            withContext(Dispatchers.IO) {
                verboseLog { "Downloading remote font from $url" }
                var connection: UrlConnection? = null
                try {
                    connection = urlConnectionFactory.createConnection(url.toString())

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("HTTP ${connection.responseCode} when downloading file at: $url")
                    }

                    return@withContext connection.inputStream
                } finally {
                    connection?.disconnect()
                }
            }
        } catch (e: IOException) {
            val message = "Failed to fetch file from remote source: $url. Error: ${e.localizedMessage}"
            logHandler.e("FileRepository", message, e)
            throw Error.FailedToFetchFileFromRemoteSource(message)
        }
    }

    private fun saveCachedFile(uri: URI, data: InputStream) {
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

private class FileCache(
    private val context: Context,
) : LargeItemCacheType {
    private val cacheDir: File by lazy {
        val dir = File(context.cacheDir, "rc_files")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    override fun generateLocalFilesystemURI(remoteURL: URL): URI? {
        // Using a simple approach of taking the last path component.
        // A more robust implementation might use a hash of the URL.
        val fileName = File(remoteURL.path).name
        if (fileName.isEmpty()) return null
        return File(cacheDir, fileName).toURI()
    }

    override fun cachedContentExists(uri: URI): Boolean =
        File(uri).exists()

    override fun saveData(data: InputStream, uri: URI) =
        writeStream(data, File(uri))

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
}
