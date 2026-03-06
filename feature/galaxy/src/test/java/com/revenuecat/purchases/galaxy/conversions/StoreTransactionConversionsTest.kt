package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.utils.parseDateFromGalaxyDateString
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class StoreTransactionConversionsTest {

    private val purchaseDateString = "2024-01-15 13:45:20"
    private val presentedOfferingContext = PresentedOfferingContext("offering_id")
    private val defaultOrderId = "order-123"
    private val defaultPurchaseId = "purchase-456"
    private lateinit var originalTimeZone: TimeZone

    @BeforeTest
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `toStoreTransaction maps core fields`() {
        val productId = "expected_product_id"
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "item",
            itemId = "original_item_id",
        )

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = productId,
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
        )

        assertThat(storeTransaction.orderId).isEqualTo(defaultOrderId)
        assertThat(storeTransaction.purchaseToken).isEqualTo(defaultPurchaseId)
        assertThat(storeTransaction.productIds).containsExactly(productId)
        assertThat(storeTransaction.purchaseState).isEqualTo(PurchaseState.PURCHASED)
        assertThat(storeTransaction.purchaseTime)
            .isEqualTo(purchaseDateString.parseDateFromGalaxyDateString().time)
        assertThat(storeTransaction.presentedOfferingContext).isEqualTo(presentedOfferingContext)
        assertThat(storeTransaction.purchaseType).isEqualTo(PurchaseType.GALAXY_PURCHASE)
        assertThat(storeTransaction.storeUserID).isNull()
        assertThat(storeTransaction.marketplace).isNull()
        assertThat(storeTransaction.signature).isNull()
        assertThat(storeTransaction.originalJson.getString("mOrderId")).isEqualTo(defaultOrderId)
        assertThat(storeTransaction.subscriptionOptionId).isNull()
        assertThat(storeTransaction.subscriptionOptionIdForProductIDs).isNull()
        assertThat(storeTransaction.replacementMode).isNull()
    }

    @Test
    fun `toStoreTransaction maps replacement mode when provided`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "subscription",
        )
        val replacementMode = GalaxyReplacementMode.INSTANT_PRORATED_DATE

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = "product",
            presentedOfferingContext = null,
            purchaseState = PurchaseState.PURCHASED,
            replacementMode = replacementMode,
        )

        assertThat(storeTransaction.replacementMode).isEqualTo(replacementMode)
    }

    @Test
    fun `toStoreTransaction uses provided product id`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "subscription",
            itemId = "original_item_id",
        )

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = "term_sku",
            presentedOfferingContext = null,
            purchaseState = PurchaseState.UNSPECIFIED_STATE,
        )

        assertThat(storeTransaction.productIds).containsExactly("term_sku")
        assertThat(storeTransaction.productIds[0]).isNotEqualTo("original_item_id")
    }

    @Test
    fun `toStoreTransaction maps subscription type and sets auto renewing`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "subscription",
        )

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = "product",
            presentedOfferingContext = null,
            purchaseState = PurchaseState.PURCHASED,
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.SUBS)
        assertThat(storeTransaction.isAutoRenewing).isTrue
    }

    @Test
    fun `toStoreTransaction maps inapp type and disables auto renewing`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "item",
        )

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = "product",
            presentedOfferingContext = null,
            purchaseState = PurchaseState.PURCHASED,
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.INAPP)
        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `toStoreTransaction maps unknown type to UNKNOWN product type`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = purchaseDateString,
            type = "weird_type",
        )

        val storeTransaction = purchaseVo.toStoreTransaction(
            productId = "product",
            presentedOfferingContext = null,
            purchaseState = PurchaseState.PENDING,
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.UNKNOWN)
        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `toStoreTransaction throws IllegalArgumentException for invalid purchase date`() {
        val purchaseVo = createMockPurchaseVo(
            orderId = defaultOrderId,
            purchaseId = defaultPurchaseId,
            purchaseDate = "not-a-date",
            type = "item",
        )

        assertThatThrownBy {
            purchaseVo.toStoreTransaction(
                productId = "product",
                presentedOfferingContext = null,
                purchaseState = PurchaseState.PURCHASED,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createMockPurchaseVo(
        orderId: String,
        purchaseId: String,
        purchaseDate: String,
        type: String,
        itemId: String = "item_id",
    ): PurchaseVo = mockk {
        every { this@mockk.type } returns type
        every { this@mockk.purchaseDate } returns purchaseDate
        every { this@mockk.orderId } returns orderId
        every { this@mockk.purchaseId } returns purchaseId
        every { this@mockk.jsonString } returns """
            {
                "mOrderId": "$orderId",
                "mPurchaseId": "$purchaseId",
                "mPurchaseDate": "$purchaseDate",
                "mType": "$type",
                "mItemId": "$itemId"
            }
        """.trimIndent()
    }
}
