package com.revenuecat.purchases.models

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * A checksum for validating file integrity
 */
@InternalRevenueCatAPI
@Serializable
data class Checksum(
    /** The algorithm used to generate the checksum */
    @SerialName("algo") val algorithm: Algorithm,

    /** The checksum hash value */
    @SerialName("value") val value: String
) {
    /**
     * Supported hashing algorithms
     */
    @Serializable
    enum class Algorithm(val algorithmName: String) {
        @SerialName("sha256") SHA256("SHA-256"),
        @SerialName("sha384") SHA384("SHA-384"),
        @SerialName("sha512") SHA512("SHA-512"),
        @SerialName("md5") MD5("MD5");

        companion object {
            fun fromString(value: String): Algorithm? = when (value.lowercase()) {
                "sha256" -> SHA256
                "sha384" -> SHA384
                "sha512" -> SHA512
                "md5" -> MD5
                else -> null
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 256 * 1024 // 256KB

        /**
         * Generate a checksum from data
         */
        fun generate(data: ByteArray, algorithm: Algorithm): Checksum {
            val digest = MessageDigest.getInstance(algorithm.algorithmName)
            val hash = digest.digest(data)
            return Checksum(algorithm, hash.toHexString())
        }

        /**
         * Generate a checksum from a file using streaming
         * (memory efficient - doesn't load entire file)
         */
        fun generate(file: File, algorithm: Algorithm): Checksum {
            val digest = MessageDigest.getInstance(algorithm.algorithmName)

            FileInputStream(file).use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val hash = digest.digest()
            return Checksum(algorithm, hash.toHexString())
        }

        /**
         * Generate checksum from an InputStream while reading it
         * (useful for validating during download)
         */
        fun generateAndConsume(
            inputStream: InputStream,
            algorithm: Algorithm,
            onData: (ByteArray, Int) -> Unit
        ): Checksum {
            val digest = MessageDigest.getInstance(algorithm.algorithmName)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
                onData(buffer, bytesRead)
            }

            val hash = digest.digest()
            return Checksum(algorithm, hash.toHexString())
        }
    }

    /**
     * Compare this checksum to another
     * @throws ChecksumValidationException if checksums don't match
     */
    fun compare(other: Checksum) {
        if (this.algorithm != other.algorithm) {
            throw ChecksumValidationException(
                "Algorithm mismatch: expected ${other.algorithm}, got ${this.algorithm}"
            )
        }

        if (this.value.lowercase() != other.value.lowercase()) {
            throw ChecksumValidationException(
                "Checksum mismatch: expected ${other.value}, got ${this.value}"
            )
        }
    }

    /**
     * Exception thrown when checksum validation fails
     */
    class ChecksumValidationException(message: String) : Exception(message)
}

internal fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}
