package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class AcknowledgePurchaseUseCaseTest : BaseBillingUseCaseTest() {

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()

    @Before
    override fun setup() {
        super.setup()
        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs
    }

    @Test
    fun `Acknowledge works`() {
        val token = "token"

        wrapper.acknowledge(
            token,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) { }

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        assertThat(capturedAcknowledgePurchaseParams.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `tokens from UNSYNCED_ACTIVE_PURCHASES are saved in cache when acknowledging`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper()
        val token = googlePurchaseWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens from PURCHASES are saved in cache when acknowledging`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper()
        val token = googlePurchaseWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are saved in cache when acknowledging`() {
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper()
        val token = historyRecordWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = historyRecordWrapper,
            initiationSource = PostReceiptInitiationSource.RESTORE,
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens are not saved in cache if acknowledge fails`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper()
        val token = googlePurchaseWrapper.purchaseToken

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult()
        )

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are not save in cache if acknowledge fails`() {
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper()
        val token = historyRecordWrapper.purchaseToken

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = historyRecordWrapper,
            initiationSource = PostReceiptInitiationSource.RESTORE
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult()
        )

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `subscriptions are acknowledged`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper()
        val token = googlePurchaseWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        val capturedAcknowledgeParams = capturedAcknowledgePurchaseParams.captured
        assertThat(capturedAcknowledgeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `restored subscriptions are acknowledged`() {
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper()
        val token = historyRecordWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = historyRecordWrapper,
            initiationSource = PostReceiptInitiationSource.RESTORE,
        )

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        val capturedAcknowledgeParams = capturedAcknowledgePurchaseParams.captured
        assertThat(capturedAcknowledgeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `if it shouldn't consume transactions, don't acknowledge and save it in cache`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper()
        val token = googlePurchaseWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = false,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `if it shouldn't consume restored transactions, don't acknowledge and save it in cache`() {
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper()
        val token = historyRecordWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = false,
            purchase = historyRecordWrapper,
            initiationSource = PostReceiptInitiationSource.RESTORE,
        )

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `Do not acknowledge purchases that are already acknowledged`() {
        val googlePurchaseWrapper = getMockedPurchaseWrapper(acknowledged = true)
        val token = googlePurchaseWrapper.purchaseToken

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(
            shouldTryToConsume = true,
            purchase = googlePurchaseWrapper,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }



    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread for purchases`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = false,
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

                acknowledgeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientDisconnectedResult,
                        )
                    } else {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientOKResult,
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = false,
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

                acknowledgeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientDisconnectedResult,
                        )
                    } else {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientOKResult,
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedToken: String? = null
        var timesExecutedInMainThread = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = false,
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

                acknowledgeStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientDisconnectedResult,
                        )
                    } else {
                        slot.captured.onAcknowledgePurchaseResponse(
                            billingClientOKResult,
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
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
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff for restores`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = true,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
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
    fun `If service returns SERVICE_UNAVAILABLE, don't retry and error if user in session for restores`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff for purchases`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = true,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
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
    fun `If service returns SERVICE_UNAVAILABLE, don't retry and error if user in session for purchases`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff for unsynced active purchases`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = true,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
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
    fun `If SERVICE_UNAVAILABLE, error if user in session acking unsynced active purchases`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry for restores`() {
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.RESTORE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.PURCHASE,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
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
        val slot = slot<AcknowledgePurchaseResponseListener>()
        val acknowledgeStubbing = every {
            mockClient.acknowledgePurchase(
                any(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = AcknowledgePurchaseUseCase(
            AcknowledgePurchaseUseCaseParams(
                "purchaseToken",
                PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                appInBackground = false,
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
                acknowledgeStubbing answers {
                    slot.captured.onAcknowledgePurchaseResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
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
        acknowledged: Boolean = false
    ): StoreTransaction {
        val p = stubGooglePurchase(
            productIds = listOf("sub"),
            purchaseToken = "token_sub",
            acknowledged = acknowledged
        )

        return p.toStoreTransaction(ProductType.SUBS, "offering_a")
    }

    private fun getMockedPurchaseHistoryRecordWrapper(): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf("sub"),
            purchaseToken = "token_sub"
        )

        return p.toStoreTransaction(
            type = ProductType.SUBS
        )
    }

}