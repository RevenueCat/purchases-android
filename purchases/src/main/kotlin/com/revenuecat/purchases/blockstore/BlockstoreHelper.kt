package com.revenuecat.purchases.blockstore

import android.content.Context
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse.BlockstoreData
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.identity.IdentityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class BlockstoreHelper(
    applicationContext: Context,
    private val identityManager: IdentityManager,
    private val blockstoreClient: BlockstoreClient = Blockstore.getClient(applicationContext),
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private companion object {
        const val BLOCKSTORE_USER_ID_KEY = "revenuecat_blockstore_user_id"
        const val BLOCKSTORE_MAX_ENTRIES = 16
    }

    fun storeUserIdIfNeeded(customerInfo: CustomerInfo) {
        if (!identityManager.currentUserIsAnonymous() || customerInfo.allPurchasedProductIds.isEmpty()) return
        ioScope.launch {
            val blockstoreData = try {
                getBlockstoreData()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog { "Failed to retrieve Block store data. Will not store userId. Error: ${e.message}" }
                return@launch
            }
            val currentUserId = identityManager.currentAppUserID
            if (shouldStoreBlockstoreUserId(blockstoreData)) {
                try {
                    storeBlockstoreUserId(currentUserId)
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    errorLog { "Failed to store user Id in Block store: $e" }
                }
            }
        }
    }

    fun recoverAndAliasBlockstoreUserIfNeeded(callback: () -> Unit) {
        val callCompletion = {
            mainScope.launch {
                callback()
            }
        }
        if (!identityManager.currentUserIsAnonymous()) {
            callCompletion()
            return
        }
        ioScope.launch {
            val blockstoreData = try {
                getBlockstoreData()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog { "Failed to retrieve Block store data. Will not recover userId. Error: ${e.message}" }
                callCompletion()
                return@launch
            }
            val currentUserId = identityManager.currentAppUserID
            val blockstoreUserId = blockstoreData[BLOCKSTORE_USER_ID_KEY]?.bytes?.let { String(it) }
            if (blockstoreUserId == null || blockstoreUserId == currentUserId) {
                callCompletion()
                return@launch
            }
            try {
                debugLog { "Aliasing Blockstore user ID: $blockstoreUserId with current UserID" }
                identityManager.aliasOldUserIdToCurrentOne(
                    oldAppUserID = blockstoreUserId,
                )
            } catch (e: PurchasesException) {
                errorLog {
                    "Failed to alias Block store user ID: ${e.message}. " +
                        "Any purchases on previous anonymous user will not be recovered."
                }
                callCompletion()
                return@launch
            }
            callCompletion()
        }
    }

    fun clearBlockstoreUserIdBackupIfNeeded(callback: () -> Unit) {
        val request = DeleteBytesRequest.Builder()
            .setKeys(listOf(BLOCKSTORE_USER_ID_KEY))
            .build()
        blockstoreClient.deleteBytes(request)
            .addOnSuccessListener {
                debugLog { "Block store cached UserID cleared if any" }
                callback()
            }
            .addOnFailureListener {
                errorLog { "Tried to clear Block store cached UserID but failed: $it" }
                callback()
            }
    }

    private suspend fun getBlockstoreData(): Map<String, BlockstoreData> {
        val retrieveRequest = RetrieveBytesRequest.Builder()
            .setRetrieveAll(true)
            .build()
        return suspendCoroutine { cont ->
            blockstoreClient.retrieveBytes(retrieveRequest)
                .addOnSuccessListener { cont.resume(it.blockstoreDataMap) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private fun shouldStoreBlockstoreUserId(blockstoreDataMap: Map<String, BlockstoreData>): Boolean {
        return blockstoreDataMap[BLOCKSTORE_USER_ID_KEY] == null && blockstoreDataMap.size < BLOCKSTORE_MAX_ENTRIES
    }

    private suspend fun storeBlockstoreUserId(userId: String): Int {
        debugLog { "Store UserID: $userId in Block store." }
        val storeRequest = StoreBytesData.Builder()
            .setBytes(userId.toByteArray())
            .setKey(BLOCKSTORE_USER_ID_KEY)
            .setShouldBackupToCloud(true)
            .build()
        return suspendCoroutine { cont ->
            blockstoreClient.storeBytes(storeRequest)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
