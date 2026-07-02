package com.revenuecat.purchases.common.security

/**
 * Defines the interface for reading, writing, updating, and deleting secure items.
 *
 * Implementations of this interface should only store items locally on device. Items should
 * not be synchronized to other devices.
 */
internal interface SecureItemStorage {

    /**
     * Determine whether the secure storage holds an item with the specified identifier.
     *
     * This method has a default implementation derived from [allItemIdentifiers].
     *
     * @param identifier the identifier of the item
     * @return `true` if the secure storage holds the item; `false` otherwise
     * @throws SecureStorageException if an error occurred during lookup
     */
    @Throws(SecureStorageException::class)
    fun containsItem(identifier: String): Boolean = allItemIdentifiers().contains(identifier)

    /**
     * Return a list of all retrievable identifiers in the secure storage.
     *
     * @return a list of identifiers
     * @throws SecureStorageException if an error occurred during retrieval
     */
    @Throws(SecureStorageException::class)
    fun allItemIdentifiers(): List<String>

    /**
     * Read a single secure item.
     *
     * @param identifier the identifier of the item
     * @return the item's data, or `null` if no item is stored for that identifier
     * @throws SecureStorageException if an error occurred during lookup
     */
    @Throws(SecureStorageException::class)
    fun readItem(identifier: String): ByteArray?

    /**
     * Save, update, or delete a single secure item.
     *
     * A `null` [contents] value causes the item to be deleted if it exists. Otherwise the item
     * is inserted or updated.
     *
     * This method has a default implementation which dispatches to [saveItem] or [deleteItem].
     *
     * @param identifier the identifier of the item
     * @param contents the new contents of the item; if `null`, the item is deleted
     * @param attributes the item's [SecureItemAttributes]; ignored when [contents] is `null`
     * @throws SecureStorageException if an error occurred during modification
     */
    @Throws(SecureStorageException::class)
    fun modifyItem(
        identifier: String,
        contents: ByteArray?,
        attributes: SecureItemAttributes = SecureItemAttributes(),
    ) {
        if (contents != null) {
            saveItem(identifier, contents, attributes)
        } else {
            deleteItem(identifier)
        }
    }

    /**
     * Save or update a single secure item.
     *
     * @param identifier the identifier of the item
     * @param contents the new or updated contents of the secure item
     * @param attributes the item's new or updated [SecureItemAttributes]
     * @throws SecureStorageException if an error occurred during saving
     */
    @Throws(SecureStorageException::class)
    fun saveItem(
        identifier: String,
        contents: ByteArray,
        attributes: SecureItemAttributes = SecureItemAttributes(),
    )

    /**
     * Delete a single secure item, if it exists.
     *
     * If no item with the specified identifier exists, this method does nothing.
     *
     * @param identifier the identifier of the item to delete
     * @throws SecureStorageException if an error occurred during deletion
     */
    @Throws(SecureStorageException::class)
    fun deleteItem(identifier: String)
}

/**
 * Storage attributes of secure items.
 */
internal data class SecureItemAttributes(

    /**
     * Whether the item should be included in Android Auto Backup.
     *
     * When `true` (the default), the item is eligible for Auto Backup and will be restored when
     * a device is set up from a backup.
     *
     * When `false`, the item is written to a separate storage file that is not included in
     * a device backup.
     *
     * This attribute does not affect whether items are accessible from other apps or devices.
     */
    val includedInBackup: Boolean = true,
)

/**
 * An exception that occurred during an operation on a [SecureItemStorage].
 */
internal class SecureStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
