package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.annotation.VisibleForTesting

private fun ByteArray.copyOf(component: Signature.Component): ByteArray {
    return copyOfRange(component.startByte, component.endByte)
}

internal data class Signature(
    val intermediateKey: ByteArray,
    val intermediateKeyExpiration: ByteArray,
    val intermediateKeySignature: ByteArray,
    val salt: ByteArray,
    val payload: ByteArray,
) {
    companion object {
        internal fun fromString(signature: String): Signature {
            val signatureBytes = Base64.decode(signature, Base64.DEFAULT)
            val expectedSize = Component.totalSize
            if (signatureBytes.size != expectedSize) {
                throw InvalidSignatureSizeException(
                    "Invalid signature size. Expected $expectedSize, " +
                        "got ${signatureBytes.size} bytes",
                )
            }
            return Signature(
                signatureBytes.copyOf(Component.INTERMEDIATE_KEY),
                signatureBytes.copyOf(Component.INTERMEDIATE_KEY_EXPIRATION),
                signatureBytes.copyOf(Component.INTERMEDIATE_KEY_SIGNATURE),
                signatureBytes.copyOf(Component.SALT),
                signatureBytes.copyOf(Component.PAYLOAD),
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal enum class Component(val size: Int) {
        INTERMEDIATE_KEY(size = 32),
        INTERMEDIATE_KEY_EXPIRATION(size = 4),
        INTERMEDIATE_KEY_SIGNATURE(size = 64),
        SALT(size = 16),
        PAYLOAD(size = 64),
        ;

        companion object {
            val totalSize: Int
                get() = values().sumOf { it.size }
        }

        val startByte: Int
            get() = values().copyOfRange(0, ordinal).sumOf { it.size }

        val endByte: Int
            get() = startByte + size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (!intermediateKey.contentEquals(other.intermediateKey)) return false
        if (!intermediateKeyExpiration.contentEquals(other.intermediateKeyExpiration)) return false
        if (!intermediateKeySignature.contentEquals(other.intermediateKeySignature)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intermediateKey.contentHashCode()
        result = 31 * result + intermediateKeyExpiration.contentHashCode()
        result = 31 * result + intermediateKeySignature.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

internal class InvalidSignatureSizeException(message: String) : Exception(message)
