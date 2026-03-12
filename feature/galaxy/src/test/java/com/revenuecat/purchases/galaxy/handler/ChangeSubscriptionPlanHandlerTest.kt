package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.conversions.toSamsungProrationMode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeSubscriptionPlanHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var changeSubscriptionPlanHandler: ChangeSubscriptionPlanHandler

    private val unexpectedOnSuccess: (PurchaseVo) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        changeSubscriptionPlanHandler = ChangeSubscriptionPlanHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `changeSubscriptionPlan errors when another request is in flight`() {
        every {
            iapHelperProvider.changeSubscriptionPlan(
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
            )
        } returns true

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_PRORATED_DATE,
            onSuccess = unexpectedOnSuccess,
            onError = unexpectedOnError,
        )

        var receivedError: PurchasesError? = null
        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("other-old"),
            newProductId = "newer",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `changeSubscriptionPlan errors when old purchase has no product id`() {
        var receivedError: PurchasesError? = null

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId(null),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_PRORATED_DATE,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_NO_OLD_PRODUCT_ID)
        verify(exactly = 0) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `changeSubscriptionPlan errors when Galaxy Store rejects request and clears in-flight`() {
        every {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
            .returnsMany(listOf(false, true))

        var receivedError: PurchasesError? = null
        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CHANGE_SUBSCRIPTION_PLAN_REQUEST)
        verify(exactly = 1) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class,
        InternalRevenueCatAPI::class
    )
    @Test
    fun `changeSubscriptionPlan dispatches request with expected args and forwards success`() {
        val oldProductId = "old-sku"
        val newProductId = "new-sku"
        val prorationMode = GalaxyReplacementMode.INSTANT_PRORATED_CHARGE
        val obfuscatedAccountIdSlot = slot<String>()
        val prorationModeSlot = slot<HelperDefine.ProrationMode>()
        every {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns true

        val onSuccess = mockk<(PurchaseVo) -> Unit>(relaxed = true)
        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId(oldProductId),
            newProductId = newProductId,
            prorationMode = prorationMode,
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        val purchaseVo = mockk<PurchaseVo>()
        changeSubscriptionPlanHandler.onChangeSubscriptionPlan(successErrorVo, purchaseVo)

        verify(exactly = 1) { onSuccess(purchaseVo) }
        verify(exactly = 1) {
            iapHelperProvider.changeSubscriptionPlan(
                oldItemId = oldProductId,
                newItemId = newProductId,
                prorationMode = capture(prorationModeSlot),
                obfuscatedAccountId = capture(obfuscatedAccountIdSlot),
                obfuscatedProfileId = null,
                onChangeSubscriptionPlanListener = changeSubscriptionPlanHandler,
            )
        }
        assertThat(prorationModeSlot.captured).isEqualTo(prorationMode.toSamsungProrationMode())
        assertThat(obfuscatedAccountIdSlot.captured).isEqualTo("user".sha256())

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId(oldProductId),
            newProductId = "next",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `changeSubscriptionPlan success with null purchase returns store problem error`() {
        every {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns true

        var receivedError: PurchasesError? = null
        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        changeSubscriptionPlanHandler.onChangeSubscriptionPlan(successErrorVo, null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_RETURNED_SUCCESS_BUT_NO_RESULT)

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `changeSubscriptionPlan error maps error and clears in-flight`() {
        every {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns true

        var receivedError: PurchasesError? = null
        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }
        changeSubscriptionPlanHandler.onChangeSubscriptionPlan(failingErrorVo, null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")

        changeSubscriptionPlanHandler.changeSubscriptionPlan(
            appUserID = "user",
            oldPurchase = transactionWithProductId("old"),
            newProductId = "new",
            prorationMode = GalaxyReplacementMode.INSTANT_NO_PRORATION,
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) {
            iapHelperProvider.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    private fun transactionWithProductId(productId: String?): StoreTransaction = mockk {
        val productIdsList = productId?.let { listOf(it) } ?: emptyList()
        every { productIds } returns productIdsList
    }
}
