package com.revenuecat.purchases.common.security

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
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
 * supplied [password] and an optional salt taken from [AccessGroup.accessGroup]. Because the
 * key is derived deterministically, it survives backup/restore — as long as the same password
 * is provided, the data is readable on any device, in contrast to Android Keystore-backed
 * approaches where keys are hardware-bound and cannot be restored.
 *
 * ## Backup behaviour
 *
 * The [SecureItemAttributes.includedInBackup] attribute controls which directory a file is
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
 * ## Cross-app sharing
 *
 * The [AccessGroup] class exists for API parity with the iOS SDK. It provides a file-name
 * namespace that prevents collisions when multiple apps from the same developer use
 * RevenueCat. It does **not** enable cross-app item sharing on Android; each app's internal
 * storage directory is OS-sandboxed.
 */
@InternalRevenueCatAPI
class EncryptedItemStorage @VisibleForTesting internal constructor(
    private val backupFile: File,
    private val noBackupFile: File,
    private val key: SecretKey,
) : SecureItemStorage {

    /**
     * An optional access-group configuration that namespaces the underlying storage files.
     *
     * Mirrors the `Keychain.AccessGroup` concept from the iOS SDK. On Android, the
     * [accessGroup] value is used as the PBKDF2 salt, so two different access groups produce
     * two different encryption keys from the same password. The [appIdentifier] is used to
     * scope the storage file names within the group.
     *
     * @param accessGroup The logical group identifier (analogous to a keychain access group
     *        entitlement on iOS). Used as the PBKDF2 salt on Android.
     * @param appIdentifier An identifier for this specific app, used to scope storage file
     *        names so that different apps within the same group do not share items.
     */
    data class AccessGroup(
        val accessGroup: String,
        val appIdentifier: String,
    )

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
         * @param context the application context
         * @param password the password from which the encryption key is derived; the caller is
         *        responsible for zeroing this array after the call returns if desired
         * @param access optional [AccessGroup] whose [AccessGroup.accessGroup] value serves as
         *        the PBKDF2 salt and whose [AccessGroup.appIdentifier] scopes file names
         * @throws GeneralSecurityException if key derivation fails
         */
        @Throws(GeneralSecurityException::class)
        fun create(
            context: Context,
            password: CharArray,
            access: AccessGroup? = null,
        ): EncryptedItemStorage {
            val salt = (access?.accessGroup ?: DEFAULT_SALT).toByteArray(Charsets.UTF_8)
            val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(spec)
                .encoded
            val key = SecretKeySpec(keyBytes, KEY_ALGORITHM)

            val baseName = if (access != null) {
                "${access.appIdentifier}_rc_secure"
            } else {
                DEFAULT_STORAGE_NAME
            }

            return EncryptedItemStorage(
                backupFile = File(context.filesDir, "${baseName}_backup.json"),
                noBackupFile = File(context.noBackupFilesDir, "${baseName}_no_backup.json"),
                key = key,
            )
        }
    }

    // Both stores are loaded once at construction time and kept in memory.
    // Reads are served entirely from these maps; writes update the map then flush the
    // affected file to disk. loadStore() is private and only called here.
    private val backupStore: MutableMap<String, String> = loadStore(backupFile)
    private val noBackupStore: MutableMap<String, String> = loadStore(noBackupFile)

    // region SecureItemStorage

    override fun containsItem(identifier: String): Boolean = synchronized(this) {
        backupStore.containsKey(identifier) || noBackupStore.containsKey(identifier)
    }

    override fun allItemIdentifiers(): List<String> = synchronized(this) {
        (backupStore.keys + noBackupStore.keys).toList()
    }

    override fun readItem(identifier: String): ByteArray? {
        // Grab the encoded ciphertext under the lock, then decrypt outside it so we don't
        // hold the monitor during a potentially slow crypto operation.
        val encoded = synchronized(this) {
            backupStore[identifier] ?: noBackupStore[identifier]
        } ?: return null
        return try {
            decrypt(Base64.decode(encoded, Base64.DEFAULT), identifier)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw SecureStorageException("Failed to read item '$identifier'", e)
        }
    }

    override fun saveItem(identifier: String, contents: ByteArray, attributes: SecureItemAttributes) {
        // Encrypt outside the lock — crypto is stateless and needs no shared state.
        val encoded = try {
            Base64.encodeToString(encrypt(contents, identifier), Base64.DEFAULT)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw SecureStorageException("Failed to save item '$identifier'", e)
        }
        val targetStore = if (attributes.includedInBackup) backupStore else noBackupStore
        val otherStore = if (attributes.includedInBackup) noBackupStore else backupStore
        val targetFile = if (attributes.includedInBackup) backupFile else noBackupFile
        val otherFile = if (attributes.includedInBackup) noBackupFile else backupFile
        synchronized(this) {
            // If the item previously lived in the other store (attributes changed), evict it so
            // it doesn't appear under both allItemIdentifiers() and readItem().
            if (otherStore.remove(identifier) != null) saveStore(otherFile, otherStore)
            targetStore[identifier] = encoded
            saveStore(targetFile, targetStore)
        }
    }

    override fun deleteItem(identifier: String) {
        synchronized(this) {
            // Only flush a file to disk if the item was actually present in it.
            val backupChanged = backupStore.remove(identifier) != null
            val noBackupChanged = noBackupStore.remove(identifier) != null
            if (backupChanged) saveStore(backupFile, backupStore)
            if (noBackupChanged) saveStore(noBackupFile, noBackupStore)
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

    private fun loadStore(file: File): MutableMap<String, String> {
        if (!file.exists()) return mutableMapOf()
        return try {
            val json = JSONObject(file.readText())
            val map = mutableMapOf<String, String>()
            for (k in json.keys()) {
                map[k] = json.getString(k)
            }
            map
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            warnLog { "Failed to load secure store from ${file.name}, starting empty: $e" }
            mutableMapOf()
        }
    }

    private fun saveStore(file: File, store: Map<String, String>) {
        val json = JSONObject()
        store.forEach { (k, v) -> json.put(k, v) }
        file.parentFile?.mkdirs()
        // Write to a temp file then rename for atomicity.
        val temp = File(file.parent, "${file.name}.tmp")
        temp.writeText(json.toString())
        temp.renameTo(file)
    }

    // endregion
}
