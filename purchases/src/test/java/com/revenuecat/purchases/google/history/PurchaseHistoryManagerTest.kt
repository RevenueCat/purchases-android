package com.revenuecat.purchases.google.history

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.vending.billing.IInAppBillingService
import com.revenuecat.purchases.ProductType
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchaseHistoryManagerTest {

    private lateinit var context: Context
    private lateinit var manager: PurchaseHistoryManager
    private lateinit var mockBillingService: IInAppBillingService
    private lateinit var serviceConnectionSlot: CapturingSlot<ServiceConnection>

    @Before
    public fun setup() {
        context = mockk()
        mockBillingService = mockk()
        serviceConnectionSlot = slot()

        every {
            context.bindService(any<Intent>(), capture(serviceConnectionSlot), any<Int>())
        } returns true

        every {
            context.unbindService(any<ServiceConnection>())
        } just Runs

        every {
            context.packageName
        } returns "com.test.package"

        manager = PurchaseHistoryManager(context)
    }

    @After
    public fun tearDown() {
        clearAllMocks()
    }

    // region connect() tests

    @Test
    fun `connect() successfully binds to service`() = runTest {
        val result = async { manager.connect() }

        // Yield to allow the async task to call bindService
        yield()

        // Simulate service connection
        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)

        assertThat(result.await()).isTrue()
        verify(exactly = 1) {
            context.bindService(
                match { intent ->
                    intent.action == BillingConstants.BILLING_SERVICE_ACTION &&
                        intent.`package` == BillingConstants.VENDING_PACKAGE
                },
                any<ServiceConnection>(),
                Context.BIND_AUTO_CREATE
            )
        }
    }

    @Test
    fun `connect() fails when bindService returns false`() = runTest {
        every {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        } returns false

        try {
            manager.connect()
            assert(false) { "Expected exception" }
        } catch (e: Exception) {
            assertThat(e.message).contains("Failed to bind to Google Play billing service")
        }
    }

    @Test
    fun `connect() throws exception when bindService throws`() = runTest {
        val expectedException = SecurityException("Permission denied")
        every {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        } throws expectedException

        try {
            manager.connect()
            assert(false) { "Expected exception" }
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(SecurityException::class.java)
            assertThat(e.message).isEqualTo("Permission denied")
        }
    }

    @Test
    fun `concurrent connect() calls reuse the same operation`() = runTest {
        val result1 = async { manager.connect() }
        val result2 = async { manager.connect() }
        val result3 = async { manager.connect() }

        // Yield to allow the async tasks to call bindService
        yield()

        // Simulate service connection
        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)

        assertThat(result1.await()).isTrue()
        assertThat(result2.await()).isTrue()
        assertThat(result3.await()).isTrue()

        // Verify bindService was only called once
        verify(exactly = 1) {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        }
    }

    @Test
    fun `connect() reuses completed connection`() = runTest {
        // First connection
        val result1 = async { manager.connect() }

        // Yield to allow the async task to call bindService
        yield()

        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)
        assertThat(result1.await()).isTrue()

        // Second connection should reuse the first
        val result2 = manager.connect()
        assertThat(result2).isTrue()

        // Verify bindService was only called once
        verify(exactly = 1) {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        }
    }

    @Test
    fun `onServiceDisconnected clears the billing service`() = runTest {
        val result = async { manager.connect() }

        // Yield to allow the async task to call bindService
        yield()

        // Simulate service connection
        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)

        assertThat(result.await()).isTrue()

        // Simulate service disconnection
        serviceConnectionSlot.captured.onServiceDisconnected(ComponentName("com.android.vending", ""))

        // Query should fail because service is null
        mockBillingServiceResponse(BillingClient.BillingResponseCode.OK, emptyList())
        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)
        assertThat(transactions).isEmpty()
    }

    // endregion

    // region queryAllPurchaseHistory() tests

    @Test
    fun `queryAllPurchaseHistory() returns empty list when service is not connected`() = runTest {
        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)
        assertThat(transactions).isEmpty()
    }

    @Test
    fun `queryAllPurchaseHistory() successfully queries INAPP products`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)
        mockBillingServiceResponse(
            BillingClient.BillingResponseCode.OK,
            listOf(purchaseData to "signature1")
        )

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        assertThat(transactions).hasSize(1)
        assertThat(transactions[0].purchaseToken).isEqualTo("token1")
        assertThat(transactions[0].productIds).containsExactly("product1")
        assertThat(transactions[0].type).isEqualTo(ProductType.INAPP)
        assertThat(transactions[0].purchaseTime).isEqualTo(1234567890000L)

        verify {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        }
    }

    @Test
    fun `queryAllPurchaseHistory() successfully queries SUBS products`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "sub1", 1234567890000L)
        mockBillingServiceResponse(
            BillingClient.BillingResponseCode.OK,
            listOf(purchaseData to "signature1")
        )

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.SUBS)

        assertThat(transactions).hasSize(1)
        assertThat(transactions[0].type).isEqualTo(ProductType.SUBS)

        verify {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.SUBS,
                null,
                any()
            )
        }
    }

    @Test
    fun `queryAllPurchaseHistory() handles pagination`() = runTest {
        connectService()

        val purchaseData1 = createMockPurchaseData("token1", "product1", 1234567890000L)
        val purchaseData2 = createMockPurchaseData("token2", "product2", 1234567891000L)
        val purchaseData3 = createMockPurchaseData("token3", "product3", 1234567892000L)

        // First call returns data with continuation token
        mockBillingServiceResponse(
            BillingClient.BillingResponseCode.OK,
            listOf(purchaseData1 to "signature1", purchaseData2 to "signature2"),
            continuationToken = "continuation_token_1"
        )

        // Second call with continuation token returns more data
        mockBillingServiceResponseWithToken(
            "continuation_token_1",
            BillingClient.BillingResponseCode.OK,
            listOf(purchaseData3 to "signature3")
        )

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        assertThat(transactions).hasSize(3)
        assertThat(transactions.map { it.purchaseToken }).containsExactly("token1", "token2", "token3")

        verify {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        }
        verify {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                "continuation_token_1",
                any()
            )
        }
    }

    @Test
    fun `queryAllPurchaseHistory() stops pagination when coroutine is cancelled`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)

        // First call returns with a delay to simulate I/O
        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), null, any())
        } answers {
            Thread.sleep(200) // Simulate I/O delay
            createBundle(
                responseCode = BillingClient.BillingResponseCode.OK,
                purchases = listOf(purchaseData to "signature1"), continuationToken = "continuation_token_1",
            )
        }

        // Run on a real dispatcher to allow proper threading
        val job = launch(Dispatchers.Default) {
            manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)
        }

        // Give some time for first page to start fetching without starting fetching the second.
        Thread.sleep(100)

        // Cancel the job while the second call is in progress
        job.cancelAndJoin()

        // Verify the first call was made
        verify(exactly = 1) {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        }

        // Verify it does not continue to the second page after cancellation
        verify(exactly = 0) {
            mockBillingService.getPurchaseHistory(
                any(),
                any(),
                any(),
                "continuation_token_1",
                any(),
            )
        }
    }

    @Test
    fun `queryAllPurchaseHistory() returns empty list on error response`() = runTest {
        connectService()

        mockBillingServiceResponse(BillingClient.BillingResponseCode.ERROR, emptyList())

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        assertThat(transactions).isEmpty()
    }

    @Test
    fun `queryAllPurchaseHistory() returns empty list on SERVICE_UNAVAILABLE response`() = runTest {
        connectService()

        mockBillingServiceResponse(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, emptyList())

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        assertThat(transactions).isEmpty()
    }

    @Test
    fun `queryAllPurchaseHistory() handles exceptions from billing service`() = runTest {
        connectService()

        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), any(), any())
        } throws RuntimeException("Test exception")

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        assertThat(transactions).isEmpty()
    }

    @Test
    fun `queryAllPurchaseHistory() skips invalid purchase data`() = runTest {
        connectService()

        val validPurchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)
        val invalidPurchaseJson = "{invalid json}"

        val bundle = Bundle().apply {
            putInt(BillingConstants.RESPONSE_CODE, BillingClient.BillingResponseCode.OK)
            putStringArrayList(
                BillingConstants.INAPP_PURCHASE_DATA_LIST,
                arrayListOf(validPurchaseData, invalidPurchaseJson)
            )
            putStringArrayList(
                BillingConstants.INAPP_DATA_SIGNATURE_LIST,
                arrayListOf("signature1", "signature2")
            )
        }

        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), any(), any())
        } returns bundle

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        // Only valid purchase should be included
        assertThat(transactions).hasSize(1)
        assertThat(transactions[0].purchaseToken).isEqualTo("token1")
    }

    @Test
    fun `concurrent queryAllPurchaseHistory() calls for same type all succeed`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)
        mockBillingServiceResponse(
            BillingClient.BillingResponseCode.OK,
            listOf(purchaseData to "signature1")
        )

        val result1 = async { manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP) }
        val result2 = async { manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP) }
        val result3 = async { manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP) }

        val transactions1 = result1.await()
        val transactions2 = result2.await()
        val transactions3 = result3.await()

        // All calls should return the same result
        assertThat(transactions1).hasSize(1)
        assertThat(transactions2).hasSize(1)
        assertThat(transactions3).hasSize(1)
        assertThat(transactions1[0].purchaseToken).isEqualTo("token1")
        assertThat(transactions2[0].purchaseToken).isEqualTo("token1")
        assertThat(transactions3[0].purchaseToken).isEqualTo("token1")
    }

    @Test
    fun `concurrent queryAllPurchaseHistory() calls for different types execute independently`() = runTest {
        connectService()

        val inappData = createMockPurchaseData("token1", "product1", 1234567890000L)
        val subsData = createMockPurchaseData("token2", "sub1", 1234567891000L)

        every {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        } returns createBundle(BillingClient.BillingResponseCode.OK, listOf(inappData to "signature1"))

        every {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.SUBS,
                null,
                any()
            )
        } returns createBundle(BillingClient.BillingResponseCode.OK, listOf(subsData to "signature2"))

        val inappResult = async { manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP) }
        val subsResult = async { manager.queryAllPurchaseHistory(BillingClient.ProductType.SUBS) }

        val inappTransactions = inappResult.await()
        val subsTransactions = subsResult.await()

        assertThat(inappTransactions).hasSize(1)
        assertThat(inappTransactions[0].productIds).containsExactly("product1")
        assertThat(subsTransactions).hasSize(1)
        assertThat(subsTransactions[0].productIds).containsExactly("sub1")

        verify(exactly = 1) {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        }
        verify(exactly = 1) {
            mockBillingService.getPurchaseHistory(
                BillingConstants.BILLING_API_VERSION,
                "com.test.package",
                BillingClient.ProductType.INAPP,
                null,
                any()
            )
        }
    }

    // endregion

    // region disconnect() tests

    @Test
    fun `disconnect() unbinds service when connected`() = runTest {
        connectService()

        manager.disconnect()

        verify {
            context.unbindService(serviceConnectionSlot.captured)
        }
    }

    @Test
    fun `disconnect() does nothing when not connected`() = runTest {
        manager.disconnect()

        verify(exactly = 0) {
            context.unbindService(any<ServiceConnection>())
        }
    }

    @Test
    fun `disconnect() handles exceptions when unbinding`() = runTest {
        connectService()

        every {
            context.unbindService(any<ServiceConnection>())
        } throws IllegalArgumentException("Service not registered")

        // Should not throw
        manager.disconnect()
    }

    @Test
    fun `disconnect() clears cached connection state`() = runTest {
        // First connection
        connectService()
        manager.disconnect()

        // Second connection should call bindService again
        val result = async { manager.connect() }

        // Yield to allow the async task to call bindService
        yield()

        assertThat(serviceConnectionSlot.isCaptured).isTrue()

        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)

        assertThat(result.await()).isTrue()

        verify(exactly = 2) {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        }
    }

    @Test
    fun `queryAllPurchaseHistory() pagination limit prevents infinite loop`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)

        // Mock infinite pagination (always returns continuationToken)
        var callCount = 0
        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), any(), any())
        } answers {
            callCount++
            createBundle(
                BillingClient.BillingResponseCode.OK,
                listOf(purchaseData to "signature$callCount"),
                continuationToken = "token$callCount" // Always return a token
            )
        }

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        // Should stop at MAX_PAGINATION_PAGES (50)
        assertThat(transactions).hasSize(50)
        assertThat(callCount).isEqualTo(50)
    }

    @Test
    fun `disconnect() handles unbind exception gracefully`() = runTest {
        connectService()

        every {
            context.unbindService(any<ServiceConnection>())
        } throws IllegalArgumentException("Service not registered")

        // Should not throw despite exception
        manager.disconnect()

        verify(exactly = 1) {
            context.unbindService(any<ServiceConnection>())
        }
    }

    @Test
    fun `service binding failures - Play Services unavailable`() = runTest {
        // Create a fresh manager to test binding failure
        val testManager = PurchaseHistoryManager(context)

        // Make bindService return false to simulate Play Services unavailable
        every {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        } returns false

        try {
            testManager.connect()
            assert(false) { "Expected exception" }
        } catch (e: Exception) {
            assertThat(e.message).contains("Failed to bind to Google Play billing service")
        }

        verify(exactly = 1) {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        }
    }

    @Test
    fun `service binding failures - SecurityException`() = runTest {
        every {
            context.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
        } throws SecurityException("Permission denied")

        try {
            manager.connect()
            assert(false) { "Expected SecurityException" }
        } catch (e: SecurityException) {
            assertThat(e.message).isEqualTo("Permission denied")
        }
    }

    @Test
    fun `service disconnects during query`() = runTest {
        connectService()

        val purchaseData = createMockPurchaseData("token1", "product1", 1234567890000L)

        var callCount = 0
        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), any(), any())
        } answers {
            callCount++
            if (callCount == 2) {
                // Trigger service disconnection during pagination
                serviceConnectionSlot.captured.onServiceDisconnected(ComponentName("com.android.vending", ""))
            }
            createBundle(
                BillingClient.BillingResponseCode.OK,
                listOf(purchaseData to "signature$callCount"),
                continuationToken = if (callCount < 5) "token$callCount" else null
            )
        }

        val transactions = manager.queryAllPurchaseHistory(BillingClient.ProductType.INAPP)

        // Should handle gracefully (returns partial results before disconnect)
        assertThat(transactions).isNotEmpty()
    }

    // endregion

    // Helper functions

    private suspend fun connectService() = coroutineScope {
        // Start connection in background
        val connectJob = async { manager.connect() }

        // Yield to allow the async task to start and call bindService
        yield()

        // Simulate service connection
        val mockIBinder = mockk<IBinder>()
        every { IInAppBillingService.Stub.asInterface(mockIBinder) } returns mockBillingService
        serviceConnectionSlot.captured.onServiceConnected(ComponentName("com.android.vending", ""), mockIBinder)

        // Wait for connection to complete
        connectJob.await()
    }

    private fun createMockPurchaseData(
        purchaseToken: String,
        productId: String,
        purchaseTime: Long
    ): String {
        return """
            {
                "orderId": "order_$purchaseToken",
                "packageName": "com.test.package",
                "productId": "$productId",
                "purchaseTime": $purchaseTime,
                "purchaseState": 0,
                "purchaseToken": "$purchaseToken",
                "quantity": 1,
                "acknowledged": false,
                "autoRenewing": false
            }
        """.trimIndent()
    }

    private fun createBundle(
        responseCode: Int,
        purchases: List<Pair<String, String>>,
        continuationToken: String? = null
    ): Bundle {
        return Bundle().apply {
            putInt(BillingConstants.RESPONSE_CODE, responseCode)
            putStringArrayList(
                BillingConstants.INAPP_PURCHASE_DATA_LIST,
                arrayListOf(*purchases.map { it.first }.toTypedArray())
            )
            putStringArrayList(
                BillingConstants.INAPP_DATA_SIGNATURE_LIST,
                arrayListOf(*purchases.map { "signature_${it.second}" }.toTypedArray())
            )
            continuationToken?.let {
                putString(BillingConstants.INAPP_CONTINUATION_TOKEN, it)
            }
        }
    }

    private fun mockBillingServiceResponse(
        responseCode: Int,
        purchases: List<Pair<String, String>>,
        continuationToken: String? = null
    ) {
        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), null, any())
        } returns createBundle(responseCode, purchases, continuationToken)
    }

    private fun mockBillingServiceResponseWithToken(
        token: String,
        responseCode: Int,
        purchases: List<Pair<String, String>>,
        continuationToken: String? = null
    ) {
        every {
            mockBillingService.getPurchaseHistory(any(), any(), any(), token, any())
        } returns createBundle(responseCode, purchases, continuationToken)
    }
}
