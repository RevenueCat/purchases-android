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
 * A [SecureItemStorage] implementation that derives an AES-256-GCM encryption key from a
 * password using PBKDF2, then stores ciphertexts as JSON files on the device's internal storage.
 *
 * ## Key derivation
 *
 * The symmetric key is derived once at initialization via `PBKDF2WithHmacSHA256` using the
 * supplied [password] and an optional salt. Because the key is derived deterministically,
 * it survives backup/restore — as long as the same password is provided, the data is readable
 * on any device, in contrast to Android Keystore-backed approaches where keys are
 * hardware-bound and cannot be restored.
 *
 * ## Backup behaviour
 *
 * The [SecureItemAttributes.includedInBackup] attribute controls which partition an item is
 * written to:
 *
 * - `true` (the default): the item is stored under [Context.getFilesDir], which participates
 *   in Android Auto Backup.
 * - `false`: the item is stored under [Context.getNoBackupFilesDir], which is explicitly
 *   excluded from Auto Backup by the OS — no additional XML configuration required.
 *
 * ## AEAD associated data
 *
 * Each item's identifier is used as AEAD associated data during encryption and decryption.
 * This means a ciphertext stored under one identifier cannot be silently decrypted as a
 * different identifier, even with the same key.
 *
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

        // 100 000 iterations meets current NIST SP 800-132 guidance. This runs once at
        // initialization, not on every read/write, so the one-time cost is acceptable.
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val KEY_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // standard nonce length for AES-GCM, in bytes
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DEFAULT_SALT = "revenuecat"
        private const val DEFAULT_STORAGE_NAME = "rc_secure"

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
            val key = withContext(computationDispatcher) {
                val saltBytes = salt.toByteArray(Charsets.UTF_8)
                val spec = PBEKeySpec(password, saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
                val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                    .generateSecret(spec)
                    .encoded
                SecretKeySpec(keyBytes, KEY_ALGORITHM)
            }

            val backupFile = File(context.filesDir, "${DEFAULT_STORAGE_NAME}_backup.json")
            val noBackupFile = File(context.noBackupFilesDir, "${DEFAULT_STORAGE_NAME}_no_backup.json")

            val (backup, noBackup) = withContext(ioDispatcher) {
                Partition(backupFile, loadStore(backupFile)) to Partition(noBackupFile, loadStore(noBackupFile))
            }

            return EncryptedItemStorage(backup, noBackup, key, computationDispatcher, ioDispatcher)
        }

        // Loads a JSON store file into a mutable map. Returns an empty map if the file does not
        // exist or cannot be parsed. Exposed internally so the test secondary constructor can
        // reuse it without duplicating the parsing logic.
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

    // Secondary constructor for tests: loads both partitions synchronously from their files.
    // Only used within the module; production code always goes through create().
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
        // Grab the encoded ciphertext under the lock, then decrypt outside it so we don't
        // hold the monitor during a potentially slow crypto operation.
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
            // Write the new disk state first — any I/O failure leaves memory unchanged so the
            // two are never left inconsistent. Memory is updated only after all writes succeed.
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

    // Writes [contents] to [file] atomically. Any I/O failure is wrapped as a
    // [SecureStorageException] so callers never need to handle raw IOException alongside it.
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
