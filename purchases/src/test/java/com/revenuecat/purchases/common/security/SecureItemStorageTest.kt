package com.revenuecat.purchases.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SecureItemStorageTest {

    // region SecureStorageException

    @Test
    fun `exception stores message`() {
        val exception = SecureStorageException("something went wrong")
        assertThat(exception.message).isEqualTo("something went wrong")
    }

    @Test
    fun `exception stores cause`() {
        val cause = RuntimeException("root cause")
        val exception = SecureStorageException("wrapper", cause)
        assertThat(exception.cause).isSameAs(cause)
    }

    @Test
    fun `exception accepts null cause`() {
        val exception = SecureStorageException("no cause", null)
        assertThat(exception.cause).isNull()
    }

    @Test
    fun `exception is throwable and catchable as Exception`() {
        assertThatThrownBy {
            throw SecureStorageException("thrown")
        }.isInstanceOf(SecureStorageException::class.java)
            .hasMessage("thrown")
    }

    @Test
    fun `exception message is not empty for any non-blank input`() {
        val exception = SecureStorageException("msg")
        assertThat(exception.message).isNotEmpty()
    }

    // endregion

    // region SecureItemAttributes

    @Test
    fun `includedInBackup defaults to true`() {
        val attributes = SecureItemAttributes()
        assertThat(attributes.includedInBackup).isTrue()
    }

    @Test
    fun `includedInBackup can be set to false`() {
        val attributes = SecureItemAttributes(includedInBackup = false)
        assertThat(attributes.includedInBackup).isFalse()
    }

    @Test
    fun `attributes are value types - copy produces independent instance`() {
        val original = SecureItemAttributes(includedInBackup = true)
        val copy = original.copy(includedInBackup = false)

        assertThat(original.includedInBackup).isTrue()
        assertThat(copy.includedInBackup).isFalse()
    }

    @Test
    fun `attributes with same values are equal`() {
        val a = SecureItemAttributes(includedInBackup = true)
        val b = SecureItemAttributes(includedInBackup = true)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `attributes with different values are not equal`() {
        val a = SecureItemAttributes(includedInBackup = true)
        val b = SecureItemAttributes(includedInBackup = false)
        assertThat(a).isNotEqualTo(b)
    }

    // endregion

    // region Default interface implementations

    @Test
    fun `containsItem returns false when storage is empty`() {
        val storage = FakeSecureItemStorage()
        assertThat(storage.containsItem("foo")).isFalse()
    }

    @Test
    fun `containsItem returns true when identifier is present`() {
        val storage = FakeSecureItemStorage()
        storage.saveItem("foo", byteArrayOf(1, 2, 3))

        assertThat(storage.containsItem("foo")).isTrue()
    }

    @Test
    fun `containsItem returns false for unknown identifier`() {
        val storage = FakeSecureItemStorage()
        storage.saveItem("foo", byteArrayOf(1))

        assertThat(storage.containsItem("bar")).isFalse()
    }

    @Test
    fun `containsItem propagates exception from allItemIdentifiers`() {
        val error = SecureStorageException("lookup failed")
        val storage = FakeSecureItemStorage(errorOnAllIdentifiers = error)

        assertThatThrownBy { storage.containsItem("foo") }
            .isInstanceOf(SecureStorageException::class.java)
            .hasMessage("lookup failed")
    }

    @Test
    fun `modifyItem with non-null contents calls saveItem`() {
        val storage = FakeSecureItemStorage()
        val data = byteArrayOf(10, 20, 30)

        storage.modifyItem("key", data)

        assertThat(storage.saveItemCallCount).isEqualTo(1)
        assertThat(storage.deleteItemCallCount).isEqualTo(0)
        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    @Test
    fun `modifyItem with null contents calls deleteItem`() {
        val storage = FakeSecureItemStorage()
        storage.saveItem("key", byteArrayOf(1))

        storage.modifyItem("key", null)

        assertThat(storage.deleteItemCallCount).isEqualTo(1)
        assertThat(storage.saveItemCallCount).isEqualTo(1) // only the setup call
        assertThat(storage.containsItem("key")).isFalse()
    }

    @Test
    fun `modifyItem uses default SecureItemAttributes when not specified`() {
        val storage = FakeSecureItemStorage()
        storage.modifyItem("key", byteArrayOf(1))

        assertThat(storage.lastSavedAttributes).isEqualTo(SecureItemAttributes())
        assertThat(storage.lastSavedAttributes?.includedInBackup).isTrue()
    }

    @Test
    fun `modifyItem forwards explicit attributes to saveItem`() {
        val storage = FakeSecureItemStorage()
        val attributes = SecureItemAttributes(includedInBackup = false)

        storage.modifyItem("key", byteArrayOf(1), attributes)

        assertThat(storage.lastSavedAttributes).isEqualTo(attributes)
    }

    @Test
    fun `saveItem convenience overload uses default SecureItemAttributes`() {
        val storage = FakeSecureItemStorage()
        storage.saveItem("key", byteArrayOf(1))

        assertThat(storage.lastSavedAttributes).isEqualTo(SecureItemAttributes())
    }

    @Test
    fun `saveItem convenience overload forwards contents correctly`() {
        val storage = FakeSecureItemStorage()
        val data = byteArrayOf(7, 8, 9)

        storage.saveItem("key", data)

        assertThat(storage.readItem("key")).isEqualTo(data)
    }

    // endregion
}

// ---------------------------------------------------------------------------
// Test double
// ---------------------------------------------------------------------------

private class FakeSecureItemStorage(
    private val errorOnAllIdentifiers: SecureStorageException? = null,
) : SecureItemStorage {

    private val items = mutableMapOf<String, Pair<ByteArray, SecureItemAttributes>>()

    var saveItemCallCount = 0
    var deleteItemCallCount = 0

    val lastSavedAttributes: SecureItemAttributes?
        get() = items.values.lastOrNull()?.second

    override fun allItemIdentifiers(): List<String> {
        errorOnAllIdentifiers?.let { throw it }
        return items.keys.toList()
    }

    override fun readItem(identifier: String): ByteArray? = items[identifier]?.first

    override fun saveItem(identifier: String, contents: ByteArray, attributes: SecureItemAttributes) {
        saveItemCallCount++
        items[identifier] = Pair(contents, attributes)
    }

    override fun deleteItem(identifier: String) {
        deleteItemCallCount++
        items.remove(identifier)
    }
}
