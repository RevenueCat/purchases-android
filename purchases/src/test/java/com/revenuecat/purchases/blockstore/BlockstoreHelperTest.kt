package com.revenuecat.purchases.blockstore

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.identity.IdentityManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BlockstoreHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockIdentityManager: IdentityManager
    private lateinit var mockBlockstoreClient: BlockstoreClient
    private lateinit var testScope: TestScope
    
    private lateinit var blockstoreHelper: BlockstoreHelper
    
    private val testAnonymousUserId = "\$RCAnonymousID:00000000000000000000000000000000"
    private val expectedKey = "com.revenuecat.purchases.app_user_id"

    @Before
    fun setup() {
        mockContext = mockk()
        mockIdentityManager = mockk<IdentityManager>().apply {
            every { currentAppUserID } returns testAnonymousUserId
        }
        mockBlockstoreClient = mockk()
        testScope = TestScope()
        
        blockstoreHelper = BlockstoreHelper(
            applicationContext = mockContext,
            identityManager = mockIdentityManager,
            blockstoreClient = mockBlockstoreClient,
            ioScope = testScope,
            mainScope = testScope
        )
    }

    // region storeUserIdIfNeeded

    @Test
    fun `storeUserIdIfNeeded does nothing when current user is not anonymous`() {
        every { mockIdentityManager.currentAppUserID } returns "not-anonymous-user-id"
        
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)
        
        verify(exactly = 0) { mockBlockstoreClient.retrieveBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded does nothing when user has no purchases`() {
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns emptySet()
        }
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)
        
        verify(exactly = 0) { mockBlockstoreClient.retrieveBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded stores user ID when conditions are met`() = runTest {
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns emptyMap()
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        val mockStoreTask = mockk<Task<Int>>()
        every { mockBlockstoreClient.storeBytes(any()) } returns mockStoreTask
        
        val storeSuccessSlot = slot<OnSuccessListener<Int>>()
        every { mockStoreTask.addOnSuccessListener(capture(storeSuccessSlot)) } returns mockStoreTask
        every { mockStoreTask.addOnFailureListener(any()) } returns mockStoreTask
        every { mockStoreTask.addOnCanceledListener(any()) } returns mockStoreTask
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)

        testScope.runCurrent()
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)

        testScope.runCurrent()
        storeSuccessSlot.captured.onSuccess(1)
        
        verify(exactly = 1) { mockBlockstoreClient.retrieveBytes(any()) }
        verify(exactly = 1) { mockBlockstoreClient.storeBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded handles retrieval failure gracefully`() = runTest {
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }
        
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveFailureSlot = slot<OnFailureListener>()
        every { mockRetrieveTask.addOnSuccessListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(capture(retrieveFailureSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)

        testScope.runCurrent()
        
        retrieveFailureSlot.captured.onFailure(RuntimeException("Test error"))
        
        verify(exactly = 1) { mockBlockstoreClient.retrieveBytes(any()) }
        verify(exactly = 0) { mockBlockstoreClient.storeBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded does not store when blockstore is full`() = runTest {
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }
        
        val fullBlockstoreMap = (1..16).associateBy(
            { "key_$it" },
            { mockk<RetrieveBytesResponse.BlockstoreData>() }
        )
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns fullBlockstoreMap
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)

        testScope.runCurrent()
        
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)

        verify(exactly = 1) { mockBlockstoreClient.retrieveBytes(any()) }
        verify(exactly = 0) { mockBlockstoreClient.storeBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded does not store when user ID already exists`() = runTest {
        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }
        
        val existingBlockstoreData = mockk<RetrieveBytesResponse.BlockstoreData>()
        val blockstoreMap = mapOf(expectedKey to existingBlockstoreData)
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns blockstoreMap
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)

        testScope.runCurrent()
        
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        
        verify(exactly = 1) { mockBlockstoreClient.retrieveBytes(any()) }
        verify(exactly = 0) { mockBlockstoreClient.storeBytes(any()) }
    }

    @Test
    fun `storeUserIdIfNeeded does nothing when no BlockstoreClient`() {
        blockstoreHelper = BlockstoreHelper(
            applicationContext = mockContext,
            identityManager = mockIdentityManager,
            blockstoreClient = null,
            ioScope = testScope,
            mainScope = testScope
        )

        val mockCustomerInfo = mockk<CustomerInfo> {
            every { allPurchasedProductIds } returns setOf("product1")
        }

        blockstoreHelper.storeUserIdIfNeeded(mockCustomerInfo)

        verify(exactly = 0) { mockIdentityManager.currentAppUserID }
        verify(exactly = 0) { mockBlockstoreClient.retrieveBytes(any()) }
    }

    // endregion storeUserIdIfNeeded

    // region aliasCurrentAndStoredUserIdsIfNeeded

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback immediately when user is not anonymous`() {
        every { mockIdentityManager.currentAppUserID } returns "not-anonymous-user-id"
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.advanceUntilIdle()

        assertThat(callbackCalled).isTrue()
        verify(exactly = 0) { mockBlockstoreClient.retrieveBytes(any()) }
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback when retrieval fails`() = runTest {
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveFailureSlot = slot<OnFailureListener>()
        every { mockRetrieveTask.addOnSuccessListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(capture(retrieveFailureSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveFailureSlot.captured.onFailure(RuntimeException("Test error"))
        testScope.advanceUntilIdle()
        
        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback when no stored user ID found`() = runTest {
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns emptyMap()
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        testScope.advanceUntilIdle()
        
        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback when stored user ID matches current`() = runTest {
        val mockBlockstoreData = mockk<RetrieveBytesResponse.BlockstoreData> {
            every { bytes } returns testAnonymousUserId.toByteArray()
        }
        val blockstoreMap = mapOf(expectedKey to mockBlockstoreData)
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns blockstoreMap
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        testScope.advanceUntilIdle()

        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded aliases different user ID successfully`() = runTest {
        val storedUserId = "stored_user_id"
        
        coEvery { mockIdentityManager.aliasCurrentUserIdTo(storedUserId) } just Runs
        
        val mockBlockstoreData = mockk<RetrieveBytesResponse.BlockstoreData> {
            every { bytes } returns storedUserId.toByteArray()
        }
        val blockstoreMap = mapOf(expectedKey to mockBlockstoreData)
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns blockstoreMap
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        testScope.advanceUntilIdle()

        assertThat(callbackCalled).isTrue()
        coVerify(exactly = 1) { mockIdentityManager.aliasCurrentUserIdTo(storedUserId) }
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded handles alias failure gracefully`() = runTest {
        val storedUserId = "stored_user_id"
        
        coEvery { mockIdentityManager.aliasCurrentUserIdTo(storedUserId) } throws
            PurchasesException(PurchasesError(PurchasesErrorCode.InvalidAppUserIdError, "Test error"))
        
        val mockBlockstoreData = mockk<RetrieveBytesResponse.BlockstoreData> {
            every { bytes } returns storedUserId.toByteArray()
        }
        val blockstoreMap = mapOf(expectedKey to mockBlockstoreData)
        
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns blockstoreMap
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask
        
        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask
        
        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        testScope.advanceUntilIdle()
        
        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback without retrieving when no BlockstoreClient`() {
        blockstoreHelper = BlockstoreHelper(
            applicationContext = mockContext,
            identityManager = mockIdentityManager,
            blockstoreClient = null,
            ioScope = testScope,
            mainScope = testScope
        )

        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.advanceUntilIdle()

        assertThat(callbackCalled).isTrue()
        verify(exactly = 0) { mockBlockstoreClient.retrieveBytes(any()) }
    }

    // endregion aliasCurrentAndStoredUserIdsIfNeeded

    // region clearUserIdBackupIfNeeded

    @Test
    fun `clearUserIdBackupIfNeeded calls delete and handles success`() {
        val mockDeleteTask = mockk<Task<Boolean>>()
        every { mockBlockstoreClient.deleteBytes(any()) } returns mockDeleteTask
        
        val deleteSuccessSlot = slot<OnSuccessListener<Boolean>>()
        every { mockDeleteTask.addOnSuccessListener(capture(deleteSuccessSlot)) } returns mockDeleteTask
        every { mockDeleteTask.addOnFailureListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnCanceledListener(any()) } returns mockDeleteTask
        
        var callbackCalled = false
        blockstoreHelper.clearUserIdBackupIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()

        deleteSuccessSlot.captured.onSuccess(true)
        testScope.runCurrent()
        
        assertThat(callbackCalled).isTrue()
        
        val deleteRequestSlot = slot<DeleteBytesRequest>()
        verify(exactly = 1) { mockBlockstoreClient.deleteBytes(capture(deleteRequestSlot)) }
        
        assertThat(deleteRequestSlot.captured.keys).containsExactly(expectedKey)
    }

    @Test
    fun `clearUserIdBackupIfNeeded calls delete and handles failure`() {
        val mockDeleteTask = mockk<Task<Boolean>>()
        every { mockBlockstoreClient.deleteBytes(any()) } returns mockDeleteTask
        
        val deleteFailureSlot = slot<OnFailureListener>()
        every { mockDeleteTask.addOnSuccessListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnFailureListener(capture(deleteFailureSlot)) } returns mockDeleteTask
        every { mockDeleteTask.addOnCanceledListener(any()) } returns mockDeleteTask
        
        var callbackCalled = false
        blockstoreHelper.clearUserIdBackupIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()

        deleteFailureSlot.captured.onFailure(RuntimeException("Test error"))
        testScope.runCurrent()
        
        assertThat(callbackCalled).isTrue()
        verify(exactly = 1) { mockBlockstoreClient.deleteBytes(any()) }
    }

    @Test
    fun `clearUserIdBackupIfNeeded does nothing if no BlockstoreClient`() {
        blockstoreHelper = BlockstoreHelper(
            applicationContext = mockContext,
            identityManager = mockIdentityManager,
            blockstoreClient = null,
            ioScope = testScope,
            mainScope = testScope
        )

        var callbackCalled = false
        blockstoreHelper.clearUserIdBackupIfNeeded {
            callbackCalled = true
        }

        assertThat(callbackCalled).isTrue()

        verify(exactly = 0) { mockBlockstoreClient.deleteBytes(any()) }
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback once when retrieval times out`() = runTest {
        val mockRetrieveResponse = mockk<RetrieveBytesResponse> {
            every { blockstoreDataMap } returns emptyMap()
        }
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask

        val retrieveSuccessSlot = slot<OnSuccessListener<RetrieveBytesResponse>>()
        every { mockRetrieveTask.addOnSuccessListener(capture(retrieveSuccessSlot)) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(any()) } returns mockRetrieveTask

        var callbackCount = 0
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCount++
        }

        testScope.runCurrent()
        assertThat(callbackCount).isZero()

        testScope.advanceTimeBy(BlockstoreHelper.BLOCKSTORE_TIMEOUT)
        testScope.runCurrent()
        assertThat(callbackCount).isEqualTo(1)

        // Late task completion after timeout must not invoke the callback again.
        retrieveSuccessSlot.captured.onSuccess(mockRetrieveResponse)
        testScope.advanceUntilIdle()
        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `aliasCurrentAndStoredUserIdsIfNeeded calls callback when retrieval is canceled`() = runTest {
        val mockRetrieveTask = mockk<Task<RetrieveBytesResponse>>()
        every { mockBlockstoreClient.retrieveBytes(any()) } returns mockRetrieveTask

        val retrieveCanceledSlot = slot<OnCanceledListener>()
        every { mockRetrieveTask.addOnSuccessListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnFailureListener(any()) } returns mockRetrieveTask
        every { mockRetrieveTask.addOnCanceledListener(capture(retrieveCanceledSlot)) } returns mockRetrieveTask

        var callbackCalled = false
        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        retrieveCanceledSlot.captured.onCanceled()
        testScope.runCurrent()

        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `clearUserIdBackupIfNeeded calls callback once when delete times out`() {
        val mockDeleteTask = mockk<Task<Boolean>>()
        every { mockBlockstoreClient.deleteBytes(any()) } returns mockDeleteTask

        val deleteSuccessSlot = slot<OnSuccessListener<Boolean>>()
        every { mockDeleteTask.addOnSuccessListener(capture(deleteSuccessSlot)) } returns mockDeleteTask
        every { mockDeleteTask.addOnFailureListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnCanceledListener(any()) } returns mockDeleteTask

        var callbackCount = 0
        blockstoreHelper.clearUserIdBackupIfNeeded {
            callbackCount++
        }

        testScope.runCurrent()
        assertThat(callbackCount).isZero()

        testScope.advanceTimeBy(BlockstoreHelper.BLOCKSTORE_TIMEOUT)
        testScope.runCurrent()
        assertThat(callbackCount).isEqualTo(1)

        // Late task completion after timeout must not invoke the callback again.
        deleteSuccessSlot.captured.onSuccess(true)
        testScope.advanceUntilIdle()
        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `clearUserIdBackupIfNeeded calls callback when delete is canceled`() {
        val mockDeleteTask = mockk<Task<Boolean>>()
        every { mockBlockstoreClient.deleteBytes(any()) } returns mockDeleteTask

        val deleteCanceledSlot = slot<OnCanceledListener>()
        every { mockDeleteTask.addOnSuccessListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnFailureListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnCanceledListener(capture(deleteCanceledSlot)) } returns mockDeleteTask

        var callbackCalled = false
        blockstoreHelper.clearUserIdBackupIfNeeded {
            callbackCalled = true
        }

        testScope.runCurrent()
        deleteCanceledSlot.captured.onCanceled()
        testScope.runCurrent()

        assertThat(callbackCalled).isTrue()
    }

    // endregion clearUserIdBackupIfNeeded
}
