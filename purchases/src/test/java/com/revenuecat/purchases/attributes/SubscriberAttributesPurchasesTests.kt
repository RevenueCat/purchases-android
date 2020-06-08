package com.revenuecat.purchases.attributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.AppConfig
import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.BillingWrapper
import com.revenuecat.purchases.IdentityManager
import com.revenuecat.purchases.PlatformInfo
import com.revenuecat.purchases.PostReceiptDataErrorCallback
import com.revenuecat.purchases.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Responses
import com.revenuecat.purchases.SubscriberAttributeError
import com.revenuecat.purchases.buildPurchaserInfo
import com.revenuecat.purchases.restorePurchasesWith
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutorService

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesPurchasesTests {

    private lateinit var underTest: Purchases
    private val appUserId = "juan"
    private val subscriberAttributesManagerMock = mockk<SubscriberAttributesManager>()
    private val backendMock = mockk<Backend>(relaxed = true)
    private val billingWrapperMock = mockk<BillingWrapper>(relaxed = true)
    private var postReceiptError: PostReceiptErrorContainer? = null
    private var postReceiptSuccess: PostReceiptSuccessContainer? = null
    private var subscriberAttribute = SubscriberAttribute("key", "value")
    private var expectedAttributes = mapOf(
        subscriberAttribute.key.backendKey to subscriberAttribute
    )

    private val attributesToMarkAsSyncSlot = slot<Map<String, SubscriberAttribute>>()
    private val attributesErrorsSlot = slot<List<SubscriberAttributeError>>()
    private val postedAttributesSlot = slot<Map<String, SubscriberAttribute>>()

    internal data class PostReceiptErrorContainer(
        val error: PurchasesError,
        val shouldConsumePurchase: Boolean,
        val subscriberAttributesErrors: List<SubscriberAttributeError>
    )

    internal data class PostReceiptSuccessContainer(
        val info: PurchaserInfo = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo(),
        val subscriberAttributeErrors: List<SubscriberAttributeError> = emptyList()
    )

    @Before
    fun setup() {
        postReceiptError = null
        postReceiptSuccess = null

        every {
            billingWrapperMock.queryAllPurchases(captureLambda(), any())
        } answers {
            lambda<(List<PurchaseHistoryRecordWrapper>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }
        val successSlot = slot<PostReceiptDataSuccessCallback>()
        val errorSlot = slot<PostReceiptDataErrorCallback>()
        every {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = capture(postedAttributesSlot),
                productInfo = any(),
                onSuccess = capture(successSlot),
                onError = capture(errorSlot)
            )
        } answers {
            postReceiptError?.let {
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.subscriberAttributesErrors)
            } ?: postReceiptSuccess?.let {
                successSlot.captured(it.info, it.subscriberAttributeErrors)
            }
        }

        every {
            subscriberAttributesManagerMock.getUnsyncedSubscriberAttributes(appUserId)
        } answers {
            expectedAttributes
        }
        every {
            subscriberAttributesManagerMock.markAsSynced(appUserId, capture(attributesToMarkAsSyncSlot), capture(attributesErrorsSlot))
        } just runs

        underTest = Purchases(
            application = mockk(relaxed = true),
            backingFieldAppUserID = appUserId,
            backend = backendMock,
            billingWrapper = billingWrapperMock,
            deviceCache = mockk(relaxed = true),
            executorService = mockk<ExecutorService>().apply {
                val capturedRunnable = slot<Runnable>()
                every { execute(capture(capturedRunnable)) } answers { capturedRunnable.captured.run() }
            },
            identityManager = mockk<IdentityManager>(relaxed = true).apply {
                every { currentAppUserID } returns appUserId
            },
            subscriberAttributesManager = subscriberAttributesManagerMock,
            appConfig = AppConfig(
                context = mockk(relaxed = true),
                observerMode = false,
                platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
                proxyURL = null
            )
        )
    }

    @Test
    fun `setting email attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val email = "email"
        underTest.setEmail(email)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.Email,
                email,
                appUserId
            )
        }
    }

    @Test
    fun `setting phone number attribute`() {
        val phoneNumber = "3154589485"

        every {
            subscriberAttributesManagerMock.setAttribute(SubscriberAttributeKey.PhoneNumber, phoneNumber, appUserId)
        } just Runs

        underTest.setPhoneNumber(phoneNumber)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.PhoneNumber,
                phoneNumber,
                appUserId
            )
        }
    }

    @Test
    fun `setting display name attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val displayName = "Cesar"
        underTest.setDisplayName(displayName)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.DisplayName,
                displayName,
                appUserId
            )
        }
    }

    @Test
    fun `setting push token attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val pushToken = "ajdjfh30203"
        underTest.setPushToken(pushToken)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.FCMTokens,
                pushToken,
                appUserId
            )
        }
    }

    @Test
    fun `on app foregrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        setup()
        underTest.onAppForegrounded()
        verify (exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        }
    }

    @Test
    fun `on app backgrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        setup()
        underTest.onAppBackgrounded()
        verify (exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        }
    }

    // region syncing purchases
    @Test
    fun `attributes are sent when syncing purchases`() {
        postReceiptSuccess = PostReceiptSuccessContainer()

        underTest.syncPurchases()

        verify (exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes,
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when syncing purchases`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptSuccess = PostReceiptSuccessContainer(
            subscriberAttributeErrors = expectedSubscriberAttributeErrors
        )
        underTest.syncPurchases()

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are marked as synced when error is finishable when syncing purchases`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            true,
            expectedSubscriberAttributeErrors
        )
        underTest.syncPurchases()

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are not marked as synced when backend did not get them when syncing purchases`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            false,
            expectedSubscriberAttributeErrors
        )
        underTest.syncPurchases()

        verify (exactly = 0) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }
    // endregion

    // region restoring purchases
    @Test
    fun `attributes are sent when restoring purchases`() {
        postReceiptSuccess = PostReceiptSuccessContainer()

        underTest.restorePurchasesWith { }

        verify (exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes,
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when restoring`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptSuccess = PostReceiptSuccessContainer(
            subscriberAttributeErrors = expectedSubscriberAttributeErrors
        )
        underTest.restorePurchasesWith { }

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are marked as synced when backend did not get them posting receipt when restoring`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            true,
            expectedSubscriberAttributeErrors
        )

        underTest.restorePurchasesWith { }

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are not marked as synced when backend did not get them posting receipt when restoring`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            false,
            expectedSubscriberAttributeErrors
        )

        underTest.restorePurchasesWith { }

        verify (exactly = 0) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }
    // endregion

    // region purchasing

    @Test
    fun `attributes are sent when purchasing`() {
        postReceiptSuccess = PostReceiptSuccessContainer()

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )

        verify (exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes,
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when purchasing`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptSuccess = PostReceiptSuccessContainer(
            subscriberAttributeErrors = expectedSubscriberAttributeErrors
        )
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are marked as synced when backend did not get them posting receipt when purchasing`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            true,
            expectedSubscriberAttributeErrors
        )

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )

        verify (exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `attributes are not marked as synced when backend did not get them posting receipt when purchasing`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            false,
            expectedSubscriberAttributeErrors
        )

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )

        verify (exactly = 0) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    // endregion

}