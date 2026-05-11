package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.models.toHexString
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal interface UrlConnectionFactory {
    fun createConnection(url: String, requestMethod: String = "GET"): UrlConnection
}

internal interface UrlConnection {
    val responseCode: Int
    val inputStream: InputStream
    fun disconnect()
}

private const val TIMEOUT = 5000

internal class DefaultUrlConnectionFactory : UrlConnectionFactory {
    override fun createConnection(url: String, requestMethod: String): UrlConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.requestMethod = requestMethod
        connection.doInput = true
        return DefaultUrlConnection(connection)
    }
}

private class DefaultUrlConnection(
    private val connection: HttpURLConnection,
) : UrlConnection {
    override val responseCode: Int
        get() = connection.responseCode
    override val inputStream: InputStream
        get() = connection.inputStream
    override fun disconnect() {
        connection.disconnect()
    }
}

private const val DOWNLOAD_BUFFER_SIZE = 256 * 1024 // 256KB

@Throws(IOException::class)
internal fun UrlConnectionFactory.downloadToFile(
    url: String,
    outputFile: File,
    description: String,
) {
    verboseLog { "Downloading $description from $url" }
    var connection: UrlConnection? = null
    try {
        connection = createConnection(url)
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException(
                "HTTP ${connection.responseCode} when downloading $description from $url",
            )
        }
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    } finally {
        connection?.disconnect()
    }
}

@OptIn(InternalRevenueCatAPI::class)
@Throws(IOException::class, Checksum.ChecksumValidationException::class)
internal fun UrlConnectionFactory.downloadToFileAndVerifyChecksum(
    url: String,
    outputFile: File,
    expectedChecksum: Checksum,
    description: String,
) {
    verboseLog { "Downloading $description from $url" }
    val digest = MessageDigest.getInstance(expectedChecksum.algorithm.algorithmName)
    var connection: UrlConnection? = null
    try {
        connection = createConnection(url)
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException(
                "HTTP ${connection.responseCode} when downloading $description from $url",
            )
        }
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                streamWithDigest(input, output, digest)
            }
        }
        val actual = Checksum(expectedChecksum.algorithm, digest.digest().toHexString())
        if (actual != expectedChecksum) {
            errorLog {
                "Downloaded $description at $url failed ${expectedChecksum.algorithm.algorithmName} verification. " +
                    "expected=${expectedChecksum.value}, actual=${actual.value}"
            }
            throw Checksum.ChecksumValidationException()
        }
    } finally {
        connection?.disconnect()
    }
}

@Throws(IOException::class)
private fun streamWithDigest(input: InputStream, output: OutputStream, digest: MessageDigest) {
    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
    var read: Int
    while (input.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
        output.write(buffer, 0, read)
    }
}
