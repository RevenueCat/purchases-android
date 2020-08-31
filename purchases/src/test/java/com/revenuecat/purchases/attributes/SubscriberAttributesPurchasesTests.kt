package com.revenuecat.purchases.attributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingWrapper
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.buildPurchaserInfo
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
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
    private var postReceiptCompletion: PostReceiptCompletionContainer? = null
    private var subscriberAttribute = SubscriberAttribute("key", "value")
    private var expectedAttributes = mapOf(
        subscriberAttribute.key.backendKey to subscriberAttribute
    )

    private val attributesToMarkAsSyncSlot = slot<Map<String, SubscriberAttribute>>()
    private val attributesErrorsSlot = slot<List<SubscriberAttributeError>>()
    private val postedAttributesSlot = slot<Map<String, Map<String, Any?>>>()

    internal data class PostReceiptErrorContainer(
        val error: PurchasesError,
        val shouldConsumePurchase: Boolean,
        val body: JSONObject?
    )

    internal data class PostReceiptCompletionContainer(
        val info: PurchaserInfo = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo(),
        val body: JSONObject? = JSONObject(Responses.validFullPurchaserResponse)
    )

    @Before
    fun setup() {
        postReceiptError = null
        postReceiptCompletion = null

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
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.body)
            } ?: postReceiptCompletion?.let {
                successSlot.captured(it.info, it.body)
            }
        }

        every {
            subscriberAttributesManagerMock.getUnsyncedSubscriberAttributes(appUserId)
        } answers {
            expectedAttributes
        }
        every {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                capture(attributesToMarkAsSyncSlot),
                capture(attributesErrorsSlot)
            )
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
        verify(exactly = 1) {
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
        verify(exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        }
    }

    // region syncing purchases
    @Test
    fun `attributes are sent when syncing purchases`() {
        postReceiptCompletion = PostReceiptCompletionContainer()

        underTest.syncPurchases()

        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when syncing purchases is successful but there are attribute errors`() {
        val expectedSubscriberAttributeErrors = getSubscriberAttributeErrorList()
        postReceiptCompletion = PostReceiptCompletionContainer(
            body = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        underTest.syncPurchases()

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `when syncing purchases, attributes are marked as synced if error is finishable`() {
        postReceiptError = getFinishableErrorResponse()

        underTest.syncPurchases()

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                emptyList()
            )
        }
    }

    @Test
    fun `when syncing purchases, attributes are not marked as synced if error is not finishable`() {
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            false,
            null
        )
        underTest.syncPurchases()

        verify(exactly = 0) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                any()
            )
        }
    }
    // endregion

    // region restoring purchases
    @Test
    fun `attributes are sent when restoring purchases`() {
        postReceiptCompletion = PostReceiptCompletionContainer()

        underTest.restorePurchasesWith { }

        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when restoring is successful but there are attribute errors`() {
        val expectedSubscriberAttributeErrors = getSubscriberAttributeErrorList()
        postReceiptCompletion = PostReceiptCompletionContainer(
            body = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        underTest.restorePurchasesWith { }

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `when restoring, attributes are marked as synced if error is finishable`() {
        postReceiptError = getFinishableErrorResponse()

        underTest.restorePurchasesWith { }

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                emptyList()
            )
        }
    }

    private fun getSubscriberAttributeErrorList(): List<SubscriberAttributeError> {
        return listOf(
            SubscriberAttributeError("invalid_name", "Attribute key name is not valid.")
        )
    }

    @Test
    fun `when restoring, attributes are not marked as synced if error is not finishable`() {
        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("key", "value")
        )
        postReceiptError = PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.NetworkError),
            false,
            null
        )

        underTest.restorePurchasesWith { }

        verify(exactly = 0) {
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
        postReceiptCompletion = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `attributes are marked as synced when posting purchase is successful but there are attribute errors`() {
        val expectedSubscriberAttributeErrors = getSubscriberAttributeErrorList()
        postReceiptCompletion = PostReceiptCompletionContainer(
            body = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    @Test
    fun `when purchasing, attributes are marked as synced if error is finishable`() {
        postReceiptError = getFinishableErrorResponse()

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                emptyList()
            )
        }
    }

    @Test
    fun `when purchasing, attributes are not marked as synced if error is not finishable`() {
        postReceiptError = PostReceiptErrorContainer(
            JSONException("exception").toPurchasesError(),
            false,
            null
        )

        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockk(relaxed = true),
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 0) {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                expectedAttributes,
                any()
            )
        }
    }

    // endregion

    private fun getFinishableErrorResponse(): PostReceiptErrorContainer {
        return PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
            true,
            JSONObject(Responses.badRequestErrorResponse)
        )
    }

    private fun getNonFinishableErrorResponse(): PostReceiptErrorContainer {
        return PostReceiptErrorContainer(
            PurchasesError(PurchasesErrorCode.UnknownError),
            true,
            JSONObject(Responses.internalServerErrorResponse)
        )
    }
}
