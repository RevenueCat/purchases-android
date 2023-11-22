package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.createPeriod
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class ConsumePurchaseUseCaseTest : BaseBillingUseCaseTest() {

    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    @Before
    override fun setup() {
        super.setup()
        mockConsumeAsync(billingClientOKResult)
    }

    @Test
    fun canConsumeAToken() {
        val token = "mockToken"

        wrapper.consumePurchase(token, PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES) {  }

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        assertThat(capturedConsumeParams.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `tokens are saved in cache when consuming`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper, PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are saved in cache when consuming`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper, PostReceiptInitiationSource.RESTORE)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens are not save in cache if consuming fails`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        mockConsumeAsync(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult())

        wrapper.consumeAndSave(true, googlePurchaseWrapper, PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are not save in cache if consuming fails`() {
        val sku = "consumable"
        val token = "token_consumable"

        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        mockConsumeAsync(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult())

        wrapper.consumeAndSave(true, historyRecordWrapper, PostReceiptInitiationSource.RESTORE)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `consumables are consumed`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper, PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue
        val capturedConsumeParams = capturedConsumeParams.captured
        assertThat(capturedConsumeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `restored consumables are consumed`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper, PostReceiptInitiationSource.RESTORE)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue
        val capturedConsumeParams = capturedConsumeParams.captured
        assertThat(capturedConsumeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `if it shouldn't consume transactions, don't consume and save it in cache`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = false,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES
        )

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `if it shouldn't consume restored transactions, don't consume and save it in cache`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = false, historyRecordWrapper, PostReceiptInitiationSource.RESTORE)

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `Do not consume nor acknowledge pending purchases`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a",
            purchaseState = Purchase.PurchaseState.PENDING
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            googlePurchaseWrapper,
            PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES
        )

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread for purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
            ),
            { received ->
                receivedToken = received
            },
            { _ ->
                Assertions.fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                timesExecutedInMainThread++

                consumeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onConsumeResponse(
                            billingClientDisconnectedResult,
                            "purchaseToken"
                        )
                    } else {
                        slot.captured.onConsumeResponse(
                            billingClientOKResult,
                            "purchaseToken"
                        )
                    }
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
        assertThat(receivedToken).isNotNull
        assertThat(receivedToken).isEqualTo("purchaseToken")
    }

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread for restores`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
            ),
            { received ->
                receivedToken = received
            },
            { _ ->
                Assertions.fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                timesExecutedInMainThread++

                consumeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onConsumeResponse(
                            billingClientDisconnectedResult,
                            "purchaseToken"
                        )
                    } else {
                        slot.captured.onConsumeResponse(
                            billingClientOKResult,
                            "purchaseToken"
                        )
                    }
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
        assertThat(receivedToken).isNotNull
        assertThat(receivedToken).isEqualTo("purchaseToken")
    }

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread for unsynced active purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            ),
            { received ->
                receivedToken = received
            },
            { _ ->
                Assertions.fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                timesExecutedInMainThread++

                consumeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onConsumeResponse(
                            billingClientDisconnectedResult,
                            "purchaseToken"
                        )
                    } else {
                        slot.captured.onConsumeResponse(
                            billingClientOKResult,
                            "purchaseToken"
                        )
                    }
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
        assertThat(receivedToken).isNotNull
        assertThat(receivedToken).isEqualTo("purchaseToken")
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute with backoff if source is unsynced active purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times if source is a purchase`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times if source is a restore`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns ERROR, re-execute with backoff if source is unsynced active purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times if source is a purchase`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times if source is a restore`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times for restores`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times for purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times for unsynced active purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry for restores`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry for purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry for unsynced active purchases`() {
        val slot = slot<ConsumeResponseListener>()
        val consumeStubbing = every {
            mockClient.consumeAsync(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = ConsumePurchaseUseCase(
            ConsumePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            ),
            { _ ->
                Assertions.fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { _, request ->
                consumeStubbing answers {
                    slot.captured.onConsumeResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        "purchaseToken"
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

    private fun getMockedPurchaseWrapper(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false
    ): StoreTransaction {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return p.toStoreTransaction(productType, offeringIdentifier)
    }

    private fun getMockedPurchaseHistoryRecordWrapper(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(productId),
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(
            type = productType
        )
    }

    private fun mockConsumeAsync(billingResult: BillingResult) {
        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }

}