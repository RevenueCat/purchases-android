package com.revenuecat.purchases.blockstore

import android.content.Context
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse.BlockstoreData
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.tasks.Task
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.safeResumeWithException
import com.revenuecat.purchases.identity.IdentityManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class BlockstoreHelper
@OptIn(ExperimentalCoroutinesApi::class)
constructor(
    applicationContext: Context,
    private val identityManager: IdentityManager,
    private val blockstoreClient: BlockstoreClient? = initializeBlockstoreClient(applicationContext),
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1)),
    private val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val blockstoreTimeout: Duration = BLOCKSTORE_TIMEOUT,
) {

    internal companion object {
        const val BLOCKSTORE_USER_ID_KEY = "com.revenuecat.purchases.app_user_id"
        const val BLOCKSTORE_MAX_ENTRIES = 16
        val BLOCKSTORE_TIMEOUT = 5.seconds

        fun initializeBlockstoreClient(applicationContext: Context): BlockstoreClient? {
            return try {
                Blockstore.getClient(applicationContext)
            } catch (e: NoClassDefFoundError) {
                // Doing this to protect against developers possibly excluding the entire
                // `com.google.android.gms` group.
                errorLog(e) { "Cannot find Blockstore at runtime. Disabling automatic backups." }
                null
            }
        }
    }

    fun storeUserIdIfNeeded(customerInfo: CustomerInfo) {
        if (blockstoreClient == null) return
        val currentUserId = identityManager.currentAppUserID
        if (
            !IdentityManager.isUserIDAnonymous(currentUserId) ||
            customerInfo.allPurchasedProductIds.isEmpty()
        ) {
            return
        }
        ioScope.launch {
            val blockstoreData = try {
                getBlockstoreData()
            } catch (e: TimeoutCancellationException) {
                errorLog(e) { "Timed out retrieving Block store data. Will not store userId." }
                return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "Failed to retrieve Block store data. Will not store userId. Error: ${e.message}" }
                return@launch
            }
            try {
                storeUserIdIfNeeded(blockstoreData, currentUserId)
            } catch (e: TimeoutCancellationException) {
                errorLog(e) { "Timed out storing user Id in Block store." }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "Failed to store user Id in Block store: ${e.message}" }
            }
        }
    }

    fun aliasCurrentAndStoredUserIdsIfNeeded(callback: () -> Unit) {
        fun callCompletion() {
            mainScope.launch {
                callback()
            }
        }
        val currentUserId = identityManager.currentAppUserID
        if (blockstoreClient == null || !IdentityManager.isUserIDAnonymous(currentUserId)) {
            callCompletion()
            return
        }
        ioScope.launch {
            val blockstoreData = try {
                getBlockstoreData()
            } catch (e: TimeoutCancellationException) {
                errorLog(e) { "Timed out retrieving Block store data. Will not recover userId." }
                callCompletion()
                return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "Failed to retrieve Block store data. Will not recover userId. Error: ${e.message}" }
                callCompletion()
                return@launch
            }
            val blockstoreUserId = blockstoreData[BLOCKSTORE_USER_ID_KEY]?.bytes?.let { String(it) }
            if (blockstoreUserId == null || blockstoreUserId == currentUserId) {
                callCompletion()
                return@launch
            }
            try {
                debugLog { "Aliasing Blockstore user ID: $blockstoreUserId with current UserID" }
                identityManager.aliasCurrentUserIdTo(
                    oldAppUserID = blockstoreUserId,
                )
            } catch (e: PurchasesException) {
                errorLog(e) {
                    "Failed to alias Block store user ID: ${e.message}. " +
                        "Underlying error: ${e.underlyingErrorMessage}. " +
                        "Any purchases on previous anonymous user will not be recovered."
                }
                callCompletion()
                return@launch
            }
            callCompletion()
        }
    }

    fun clearUserIdBackupIfNeeded(callback: () -> Unit) {
        val blockstoreClient = this.blockstoreClient ?: run {
            callback()
            return
        }
        val request = DeleteBytesRequest.Builder()
            .setKeys(listOf(BLOCKSTORE_USER_ID_KEY))
            .build()
        ioScope.launch {
            try {
                blockstoreClient.deleteBytes(request).awaitWithTimeout()
                debugLog { "Block store cached UserID cleared if any" }
            } catch (e: TimeoutCancellationException) {
                errorLog(e) { "Timed out clearing Block store cached UserID." }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "Tried to clear Block store cached UserID but failed: ${e.message}" }
            }
            callback()
        }
    }

    private suspend fun getBlockstoreData(): Map<String, BlockstoreData> {
        val blockstoreClient = this.blockstoreClient ?: return emptyMap()
        val retrieveRequest = RetrieveBytesRequest.Builder()
            .setRetrieveAll(true)
            .build()
        return blockstoreClient.retrieveBytes(retrieveRequest).awaitWithTimeout().blockstoreDataMap
    }

    @Suppress("ReturnCount")
    private suspend fun storeUserIdIfNeeded(
        blockstoreDataMap: Map<String, BlockstoreData>,
        userId: String,
    ) {
        val blockstoreClient = this.blockstoreClient ?: return
        if (blockstoreDataMap[BLOCKSTORE_USER_ID_KEY] != null) {
            debugLog { "Block store: Not storing user id since there is one already present." }
            return
        }
        if (blockstoreDataMap.size >= BLOCKSTORE_MAX_ENTRIES) {
            debugLog { "Block store: Not storing user id since block store is already full." }
            return
        }
        debugLog { "Block store: Storing UserID: $userId in Block store." }
        val storeRequest = StoreBytesData.Builder()
            .setBytes(userId.toByteArray())
            .setKey(BLOCKSTORE_USER_ID_KEY)
            .setShouldBackupToCloud(true)
            .build()
        blockstoreClient.storeBytes(storeRequest).awaitWithTimeout()
        debugLog { "Block store: User ID: $userId stored in Block store." }
    }

    private suspend fun <T> Task<T>.awaitWithTimeout(): T = withTimeout(blockstoreTimeout) {
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.safeResume(it) }
            addOnFailureListener { cont.safeResumeWithException(it) }
            addOnCanceledListener {
                cont.safeResumeWithException(IllegalStateException("Block store task was canceled."))
            }
        }
    }
}
