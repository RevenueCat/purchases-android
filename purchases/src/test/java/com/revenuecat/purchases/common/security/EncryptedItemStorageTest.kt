package com.revenuecat.purchases.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Tests for [EncryptedItemStorage] using temp files and a randomly generated [SecretKey]
 * injected via the `@VisibleForTesting` constructor. This exercises all business logic
 * (routing, attribute handling, encryption round-trips, file I/O, AccessGroup namespacing)
 * without requiring a real PBKDF2 derivation. The key derivation itself is a one-liner
 * delegating to the JDK's SecretKeyFactory, which has its own test coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EncryptedItemStorageTest {

    // A randomly generated AES-256 key shared across all storage instances in a test.
    // Two instances that share a key (and the same files) can decrypt each other's output.
    private val testKey: SecretKey = KeyGenerator.getInstance("AES")
        .also { it.init(256) }
        .generateKey()

    private lateinit var tempDir: File
    private lateinit var backupFile: File
    private lateinit var noBackupFile: File
    private lateinit var storage: EncryptedItemStorage

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "EncryptedItemStorageTest_${System.nanoTime()}")
        tempDir.mkdirs()
        backupFile = File(tempDir, "backup.json")
        noBackupFile = File(tempDir, "no_backup.json")
        storage = EncryptedItemStorage(backupFile, noBackupFile, testKey)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // Returns a storage instance that can only see items in [backupFile] (the no-backup slot
    // points at a non-existent file). Used to verify per-file routing.
    private fun backupOnly() =
        EncryptedItemStorage(backupFile, File(tempDir, "empty.json"), testKey)

    // Returns a storage instance that can only see items in [noBackupFile].
    private fun noBackupOnly() =
        EncryptedItemStorage(File(tempDir, "empty.json"), noBackupFile, testKey)

    private fun makeStorage(backupName: String, noBackupName: String) =
        EncryptedItemStorage(
            File(tempDir, "$backupName.json"),
            File(tempDir, "$noBackupName.json"),
            testKey,
        )

    // region AccessGroup

    @Test
    fun `AccessGroup stores accessGroup and appIdentifier`() {
        val group = EncryptedItemStorage.AccessGroup(
            accessGroup = "com.example.group",
            appIdentifier = "com.example.myapp",
        )
        assertThat(group.accessGroup).isEqualTo("com.example.group")
        assertThat(group.appIdentifier).isEqualTo("com.example.myapp")
    }

    @Test
    fun `AccessGroup is a value type - copy produces independent instance`() {
        val original = EncryptedItemStorage.AccessGroup(
            accessGroup = "group",
            appIdentifier = "app",
        )
        val copy = original.copy(appIdentifier = "other-app")
        assertThat(original.appIdentifier).isEqualTo("app")
        assertThat(copy.appIdentifier).isEqualTo("other-app")
    }

    @Test
    fun `AccessGroup instances with same values are equal`() {
        val a = EncryptedItemStorage.AccessGroup("group", "app")
        val b = EncryptedItemStorage.AccessGroup("group", "app")
        assertThat(a).isEqualTo(b)
    }

    // endregion

    // region File namespace isolation

    @Test
    fun `two storages with different backing files do not share items`() {
        val storageA = makeStorage("app_a_backup", "app_a_no_backup")
        val storageB = makeStorage("app_b_backup", "app_b_no_backup")

        storageA.saveItem("token", byteArrayOf(1, 2, 3))

        assertThat(storageA.containsItem("token")).isTrue()
        assertThat(storageB.containsItem("token")).isFalse()
    }

    @Test
    fun `two storages sharing the same files and key share items`() {
        // storage2 is constructed after storage writes, so it loads the populated file.
        // This reflects normal usage: a second instance created from the same configuration
        // (e.g. after process restart) can read data written by an earlier instance.
        storage.saveItem("token", byteArrayOf(9, 8, 7))
        val storage2 = EncryptedItemStorage(backupFile, noBackupFile, testKey)

        assertThat(storage2.containsItem("token")).isTrue()
        assertThat(storage2.readItem("token")).isEqualTo(byteArrayOf(9, 8, 7))
    }

    // endregion

    // region containsItem

    @Test
    fun `containsItem returns false for non-existent item`() {
        assertThat(storage.containsItem("missing")).isFalse()
    }

    @Test
    fun `containsItem returns true after save with default attributes`() {
        storage.saveItem("key", byteArrayOf(1))
        assertThat(storage.containsItem("key")).isTrue()
    }

    @Test
    fun `containsItem returns true for item in backup file`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = true))
        assertThat(storage.containsItem("key")).isTrue()
    }

    @Test
    fun `containsItem returns true for item in no-backup file`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = false))
        assertThat(storage.containsItem("key")).isTrue()
    }

    @Test
    fun `containsItem returns false after delete`() {
        storage.saveItem("key", byteArrayOf(1))
        storage.deleteItem("key")
        assertThat(storage.containsItem("key")).isFalse()
    }

    // endregion

    // region allItemIdentifiers

    @Test
    fun `allItemIdentifiers returns empty list when storage is empty`() {
        assertThat(storage.allItemIdentifiers()).isEmpty()
    }

    @Test
    fun `allItemIdentifiers returns identifiers from backup file`() {
        storage.saveItem("alpha", byteArrayOf(1), SecureItemAttributes(includedInBackup = true))
        storage.saveItem("beta", byteArrayOf(2), SecureItemAttributes(includedInBackup = true))
        assertThat(storage.allItemIdentifiers()).containsExactlyInAnyOrder("alpha", "beta")
    }

    @Test
    fun `allItemIdentifiers returns identifiers from no-backup file`() {
        storage.saveItem("gamma", byteArrayOf(3), SecureItemAttributes(includedInBackup = false))
        assertThat(storage.allItemIdentifiers()).containsExactlyInAnyOrder("gamma")
    }

    @Test
    fun `allItemIdentifiers merges identifiers from both files`() {
        storage.saveItem("backed", byteArrayOf(1), SecureItemAttributes(includedInBackup = true))
        storage.saveItem("local", byteArrayOf(2), SecureItemAttributes(includedInBackup = false))
        assertThat(storage.allItemIdentifiers()).containsExactlyInAnyOrder("backed", "local")
    }

    @Test
    fun `allItemIdentifiers does not include deleted items`() {
        storage.saveItem("keep", byteArrayOf(1))
        storage.saveItem("remove", byteArrayOf(2))
        storage.deleteItem("remove")
        assertThat(storage.allItemIdentifiers()).containsExactly("keep")
    }

    // endregion

    // region readItem

    @Test
    fun `readItem returns null for non-existent item`() {
        assertThat(storage.readItem("missing")).isNull()
    }

    @Test
    fun `readItem returns correct data after save`() {
        val data = byteArrayOf(10, 20, 30, 40)
        storage.saveItem("key", data)
        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    @Test
    fun `readItem returns data from backup file`() {
        val data = byteArrayOf(5, 6)
        storage.saveItem("key", data, SecureItemAttributes(includedInBackup = true))
        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    @Test
    fun `readItem returns data from no-backup file`() {
        val data = byteArrayOf(7, 8)
        storage.saveItem("key", data, SecureItemAttributes(includedInBackup = false))
        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    @Test
    fun `readItem returns null after delete`() {
        storage.saveItem("key", byteArrayOf(1))
        storage.deleteItem("key")
        assertThat(storage.readItem("key")).isNull()
    }

    @Test
    fun `readItem handles empty ByteArray`() {
        storage.saveItem("key", byteArrayOf())
        assertThat(storage.readItem("key")).isEqualTo(byteArrayOf())
    }

    @Test
    fun `readItem handles all 256 byte values`() {
        val allBytes = ByteArray(256) { it.toByte() }
        storage.saveItem("key", allBytes)
        assertThat(storage.readItem("key")).isEqualTo(allBytes)
    }

    @Test
    fun `readItem handles large payload`() {
        val largeData = ByteArray(65_536) { (it % 256).toByte() }
        storage.saveItem("key", largeData)
        assertThat(storage.readItem("key")).isEqualTo(largeData)
    }

    // endregion

    // region saveItem

    @Test
    fun `saveItem overwrites an existing item`() {
        storage.saveItem("key", byteArrayOf(1, 2, 3))
        storage.saveItem("key", byteArrayOf(9, 9, 9))
        assertThat(storage.readItem("key")).isEqualTo(byteArrayOf(9, 9, 9))
    }

    @Test
    fun `saveItem routes to backup file by default`() {
        storage.saveItem("key", byteArrayOf(1))
        assertThat(backupOnly().readItem("key")).isEqualTo(byteArrayOf(1))
        assertThat(noBackupOnly().readItem("key")).isNull()
    }

    @Test
    fun `saveItem routes to no-backup file when includedInBackup is false`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = false))
        assertThat(noBackupOnly().readItem("key")).isEqualTo(byteArrayOf(1))
        assertThat(backupOnly().readItem("key")).isNull()
    }

    @Test
    fun `saveItem moves item from backup to no-backup file when attributes change`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = true))
        assertThat(backupOnly().containsItem("key")).isTrue()

        storage.saveItem("key", byteArrayOf(2), SecureItemAttributes(includedInBackup = false))

        assertThat(noBackupOnly().readItem("key")).isEqualTo(byteArrayOf(2))
        assertThat(backupOnly().containsItem("key")).isFalse()
    }

    @Test
    fun `saveItem moves item from no-backup to backup file when attributes change`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = false))
        assertThat(noBackupOnly().containsItem("key")).isTrue()

        storage.saveItem("key", byteArrayOf(2), SecureItemAttributes(includedInBackup = true))

        assertThat(backupOnly().readItem("key")).isEqualTo(byteArrayOf(2))
        assertThat(noBackupOnly().containsItem("key")).isFalse()
    }

    @Test
    fun `saveItem with unicode identifier stores and retrieves correctly`() {
        val identifier = "用户令牌_🔐"
        storage.saveItem(identifier, byteArrayOf(99))
        assertThat(storage.containsItem(identifier)).isTrue()
        assertThat(storage.readItem(identifier)).isEqualTo(byteArrayOf(99))
    }

    @Test
    fun `saveItem with whitespace-only identifier stores and retrieves correctly`() {
        val identifier = "   "
        storage.saveItem(identifier, byteArrayOf(55))
        assertThat(storage.containsItem(identifier)).isTrue()
    }

    // endregion

    // region deleteItem

    @Test
    fun `deleteItem is a no-op for non-existent item`() {
        storage.deleteItem("missing") // must not throw
        assertThat(storage.allItemIdentifiers()).isEmpty()
    }

    @Test
    fun `deleteItem removes an item from backup file`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = true))
        storage.deleteItem("key")
        assertThat(storage.containsItem("key")).isFalse()
    }

    @Test
    fun `deleteItem removes an item from no-backup file`() {
        storage.saveItem("key", byteArrayOf(1), SecureItemAttributes(includedInBackup = false))
        storage.deleteItem("key")
        assertThat(storage.containsItem("key")).isFalse()
    }

    @Test
    fun `deleteItem does not remove other items`() {
        storage.saveItem("keep", byteArrayOf(1))
        storage.saveItem("remove", byteArrayOf(2))
        storage.deleteItem("remove")
        assertThat(storage.containsItem("keep")).isTrue()
        assertThat(storage.readItem("keep")).isEqualTo(byteArrayOf(1))
    }

    // endregion

    // region modifyItem

    @Test
    fun `modifyItem with non-null contents saves the item`() {
        val data = byteArrayOf(11, 22)
        storage.modifyItem("key", data)
        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    @Test
    fun `modifyItem with null contents deletes the item`() {
        storage.saveItem("key", byteArrayOf(1))
        storage.modifyItem("key", null)
        assertThat(storage.containsItem("key")).isFalse()
    }

    @Test
    fun `modifyItem with null contents on non-existent item is a no-op`() {
        storage.modifyItem("missing", null) // must not throw
        assertThat(storage.allItemIdentifiers()).isEmpty()
    }

    @Test
    fun `modifyItem routes to no-backup file when specified`() {
        storage.modifyItem("key", byteArrayOf(3), SecureItemAttributes(includedInBackup = false))
        assertThat(noBackupOnly().readItem("key")).isEqualTo(byteArrayOf(3))
        assertThat(backupOnly().readItem("key")).isNull()
    }

    // endregion
}
