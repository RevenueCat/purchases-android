package com.revenuecat.purchases.common.security

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.toMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A [SecureItemStorage] implementation that derives an AES-256-GCM key from a password via
 * PBKDF2, and stores ciphertexts as JSON files under a `RevenueCat` subfolder.
 *
 * The key is derived deterministically from [password] + salt, so it survives backup/restore,
 * unlike Android Keystore-backed keys. [SecureItemAttributes.includedInBackup] picks the
 * partition: `true` uses [Context.getFilesDir] (Auto Backup eligible), `false` uses
 * [Context.getNoBackupFilesDir] (excluded). Each item's identifier is used as AEAD associated
 * data, so a ciphertext can't be silently decrypted under a different identifier.
 */
internal class EncryptedItemStorage private constructor(
    private val backup: Partition,
    private val noBackup: Partition,
    private val key: SecretKey,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SecureItemStorage {

    // Pairs a backing file with its in-memory map of encrypted entries.
    private data class Partition(val file: File, val contents: MutableMap<String, String>)

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        // Meets NIST SP 800-132 guidance; runs once at init, so the cost is acceptable.
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val KEY_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // standard nonce length for AES-GCM, in bytes
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DEFAULT_SALT = "revenuecat"
        private const val DEFAULT_STORAGE_NAME = "rc_secure"

        // Groups this SDK's files, mirroring ETagPayloadStore/RemoteConfigDiskCache/RemoteConfigBlobStore.
        private const val VENDOR_DIRECTORY = "RevenueCat"

        // Shared key derivation for create()/createBlocking() -- CPU-bound.
        @Throws(GeneralSecurityException::class)
        private fun deriveKey(password: CharArray, salt: String): SecretKey {
            val saltBytes = salt.toByteArray(Charsets.UTF_8)
            val spec = PBEKeySpec(password, saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(spec)
                .encoded
            return SecretKeySpec(keyBytes, KEY_ALGORITHM)
        }

        // Shared partition file load for create()/createBlocking().
        private fun loadPartitions(context: Context): Pair<Partition, Partition> {
            val backupFile = File(
                File(context.filesDir, VENDOR_DIRECTORY),
                "${DEFAULT_STORAGE_NAME}_backup.json",
            )
            val noBackupFile = File(
                File(context.noBackupFilesDir, VENDOR_DIRECTORY),
                "${DEFAULT_STORAGE_NAME}_no_backup.json",
            )
            return Partition(backupFile, loadStore(backupFile)) to Partition(noBackupFile, loadStore(noBackupFile))
        }

        /**
         * Create an [EncryptedItemStorage] backed by PBKDF2-derived AES-256-GCM.
         *
         * Key derivation runs on [computationDispatcher] (CPU-bound); store loading runs on
         * [ioDispatcher]. Neither blocks the calling thread.
         *
         * @param context the application context
         * @param password the password from which the encryption key is derived; the caller is
         *        responsible for zeroing this array after the call returns if desired
         * @param salt the PBKDF2 salt; defaults to [DEFAULT_SALT]
         * @param computationDispatcher dispatcher for CPU-bound work; defaults to [Dispatchers.Default]
         * @param ioDispatcher dispatcher for I/O-bound work; defaults to [Dispatchers.IO]
         * @throws GeneralSecurityException if key derivation fails
         */
        @Throws(GeneralSecurityException::class)
        suspend fun create(
            context: Context,
            password: CharArray,
            salt: String = DEFAULT_SALT,
            computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): EncryptedItemStorage {
            val key = withContext(computationDispatcher) { deriveKey(password, salt) }
            val (backup, noBackup) = withContext(ioDispatcher) { loadPartitions(context) }
            return EncryptedItemStorage(backup, noBackup, key, computationDispatcher, ioDispatcher)
        }

        /**
         * Synchronous twin of [create], for a caller already on its own background thread (only
         * [TokenManager]) that has no need for [create]'s dispatcher hops.
         *
         * @throws GeneralSecurityException if key derivation fails
         */
        @Throws(GeneralSecurityException::class)
        fun createBlocking(
            context: Context,
            password: CharArray,
            salt: String = DEFAULT_SALT,
        ): EncryptedItemStorage {
            val key = deriveKey(password, salt)
            val (backup, noBackup) = loadPartitions(context)
            return EncryptedItemStorage(backup, noBackup, key)
        }

        // Loads file into a map, or empty if missing/unparseable. internal for the test constructor.
        internal fun loadStore(file: File): MutableMap<String, String> {
            if (!file.exists()) return mutableMapOf()
            return try {
                val bytes = AtomicFile(file).readFully()
                JSONObject(String(bytes, Charsets.UTF_8)).toMap<String>().toMutableMap()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                warnLog { "Failed to load secure store from ${file.name}, starting empty: $e" }
                mutableMapOf()
            }
        }
    }

    // Test-only constructor: loads both partitions synchronously from their files.
    internal constructor(
        backupFile: File,
        noBackupFile: File,
        key: SecretKey,
        computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(
        backup = Partition(backupFile, loadStore(backupFile)),
        noBackup = Partition(noBackupFile, loadStore(noBackupFile)),
        key = key,
        computationDispatcher = computationDispatcher,
        ioDispatcher = ioDispatcher,
    )

    // region SecureItemStorage

    override fun containsItem(identifier: String): Boolean = synchronized(this) {
        backup.contents.containsKey(identifier) || noBackup.contents.containsKey(identifier)
    }

    override fun allItemIdentifiers(): List<String> = synchronized(this) {
        (backup.contents.keys + noBackup.contents.keys).toList()
    }

    override fun readItem(identifier: String): ByteArray? {
        // Grab ciphertext under the lock, decrypt outside it (avoid holding the monitor during crypto).
        val encoded = synchronized(this) {
            backup.contents[identifier] ?: noBackup.contents[identifier]
        } ?: return null
        return try {
            decrypt(Base64.decode(encoded, Base64.NO_WRAP), identifier)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw SecureStorageException("Failed to read item '$identifier'", e)
        }
    }

    override fun saveItem(identifier: String, contents: ByteArray, attributes: SecureItemAttributes) {
        // Encrypt outside the lock — crypto is stateless and needs no shared state.
        val encoded = try {
            Base64.encodeToString(encrypt(contents, identifier), Base64.NO_WRAP)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw SecureStorageException("Failed to save item '$identifier'", e)
        }
        val target = if (attributes.includedInBackup) backup else noBackup
        val other = if (attributes.includedInBackup) noBackup else backup
        synchronized(this) {
            // Write disk first -- an I/O failure then leaves memory unchanged.
            val evictingFromOther = other.contents.containsKey(identifier)
            if (evictingFromOther) saveContents(other.file, other.contents - identifier)
            saveContents(target.file, target.contents + (identifier to encoded))
            if (evictingFromOther) other.contents.remove(identifier)
            target.contents[identifier] = encoded
        }
    }

    override fun deleteItem(identifier: String) {
        synchronized(this) {
            // Write the new disk state first — any I/O failure leaves memory unchanged.
            val inBackup = backup.contents.containsKey(identifier)
            val inNoBackup = noBackup.contents.containsKey(identifier)
            if (inBackup) saveContents(backup.file, backup.contents - identifier)
            if (inNoBackup) saveContents(noBackup.file, noBackup.contents - identifier)
            if (inBackup) backup.contents.remove(identifier)
            if (inNoBackup) noBackup.contents.remove(identifier)
        }
    }

    // endregion

    // region Crypto

    private fun encrypt(plaintext: ByteArray, identifier: String): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(identifier.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext)
        return cipher.iv + ciphertext // prepend the 12-byte IV to the ciphertext
    }

    private fun decrypt(data: ByteArray, identifier: String): ByteArray {
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(identifier.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }

    // endregion

    // region File I/O

    // Writes [contents] to [file] atomically; wraps I/O failure as [SecureStorageException].
    @Throws(SecureStorageException::class)
    private fun saveContents(file: File, contents: Map<String, String>) {
        val json = JSONObject()
        contents.forEach { (k, v) -> json.put(k, v) }
        file.parentFile?.mkdirs()
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(json.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            atomicFile.failWrite(stream)
            throw SecureStorageException("Failed to write secure store to ${file.name}", e)
        }
    }

    // endregion
}
