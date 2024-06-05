@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.InstallmentPlanDetails
import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.json.JSONArray

@SuppressWarnings("LongParameterList", "MagicNumber")
fun mockProductDetails(
    productId: String = "sample_product_id",
    @BillingClient.ProductType type: String = BillingClient.ProductType.SUBS,
    oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails? = null,
    subscriptionOfferDetails: List<SubscriptionOfferDetails>? = listOf(mockSubscriptionOfferDetails()),
    name: String = "subscription_mock_name",
    description: String = "subscription_mock_description",
    title: String = "subscription_mock_title",
): ProductDetails = mockk<ProductDetails>().apply {
    every { getProductId() } returns productId
    every { productType } returns type
    every { getName() } returns name
    every { getDescription() } returns description
    every { getTitle() } returns title
    every { getOneTimePurchaseOfferDetails() } returns oneTimePurchaseOfferDetails
    every { getSubscriptionOfferDetails() } returns subscriptionOfferDetails
    every { zza() } returns "mock-package-name" // This seems to return the packageName property from the response json
}

@SuppressWarnings("MagicNumber")
fun mockOneTimePurchaseOfferDetails(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
): OneTimePurchaseOfferDetails = mockk<OneTimePurchaseOfferDetails>().apply {
    every { formattedPrice } returns "${'$'}$price"
    every { priceAmountMicros } returns price.times(1_000_000).toLong()
    every { priceCurrencyCode } returns priceCurrencyCodeValue
}

fun mockSubscriptionOfferDetails(
    tags: List<String> = emptyList(),
    token: String = "mock-subscription-offer-token",
    offerId: String = "mock-offer-id",
    basePlanId: String = "mock-base-plan-id",
    pricingPhases: List<PricingPhase> = listOf(mockPricingPhase()),
    installmentDetails: ProductDetails.InstallmentPlanDetails? = null,
): SubscriptionOfferDetails = mockk<SubscriptionOfferDetails>().apply {
    every { offerTags } returns tags
    every { offerToken } returns token
    every { getOfferId() } returns offerId
    every { getBasePlanId() } returns basePlanId
    every { getPricingPhases() } returns mockk<ProductDetails.PricingPhases>().apply {
        every { pricingPhaseList } returns pricingPhases
    }
    every { installmentPlanDetails } returns installmentDetails
}

fun mockInstallmentPlandetails(
    commitmentPaymentsCount: Int = 3,
    subsequentCommitmentPaymentsCount: Int = 1,
): InstallmentPlanDetails {
    return mockk<InstallmentPlanDetails>().apply {
        every { installmentPlanCommitmentPaymentsCount } returns commitmentPaymentsCount
        every { subsequentInstallmentPlanCommitmentPaymentsCount } returns subsequentCommitmentPaymentsCount
    }
}

@SuppressWarnings("MagicNumber")
fun mockPricingPhase(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
    billingPeriod: String = "P1M",
    billingCycleCount: Int = 0,
    recurrenceMode: Int = RecurrenceMode.INFINITE_RECURRING,
): PricingPhase = mockk<PricingPhase>().apply {
    every { formattedPrice } returns "${'$'}$price"
    every { priceAmountMicros } returns price.times(1_000_000).toLong()
    every { priceCurrencyCode } returns priceCurrencyCodeValue
    every { getBillingPeriod() } returns billingPeriod
    every { getBillingCycleCount() } returns billingCycleCount
    every { getRecurrenceMode() } returns recurrenceMode
}

fun createMockProductDetailsNoOffers(): ProductDetails = mockProductDetails()
fun createMockProductDetailsFreeTrial(
    productId: String = "mock-free-trial-subscription",
    priceAfterFreeTrial: Double = 4.99,
    freeTrialPeriod: String = "P7D",
    subscriptionPeriod: String = "P1M",
): ProductDetails = mockProductDetails(
    productId = productId,
    subscriptionOfferDetails = listOf(
        mockSubscriptionOfferDetails(
            token = "free_trial_offer",
            pricingPhases = listOf(
                mockPricingPhase(
                    price = 0.0,
                    billingCycleCount = 1,
                    billingPeriod = freeTrialPeriod,
                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                ),
                mockPricingPhase(
                    price = priceAfterFreeTrial,
                    billingPeriod = subscriptionPeriod,
                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                ),
            ),
        ),
        mockSubscriptionOfferDetails(
            token = "base_plan_offer",
            pricingPhases = listOf(
                mockPricingPhase(
                    price = priceAfterFreeTrial,
                    billingPeriod = subscriptionPeriod,
                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                ),
            ),
        ),
    ),
)
fun createMockOneTimeProductDetails(productId: String, price: Double = 4.99): ProductDetails = mockProductDetails(
    productId = productId,
    type = BillingClient.ProductType.INAPP,
    oneTimePurchaseOfferDetails = mockOneTimePurchaseOfferDetails(
        price = price,
    ),
    subscriptionOfferDetails = null,
)

@SuppressWarnings("LongParameterList", "MagicNumber")
fun stubGooglePurchase(
    productIds: List<String> = listOf("com.revenuecat.lifetime"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijipehfnbbnldmai.AO-J1OxqriTepvB7suzlIhxqPIveA0IHtX9amMedK0KK9CsO0S3Zk5H6gdwvV" +
        "7HzZIJeTzqkY4okyVk8XKTmK1WZKAKSNTKop4dgwSmFnLWsCxYbahUmADg",
    signature: String = "signature${System.currentTimeMillis()}",
    purchaseState: Int = Purchase.PurchaseState.PURCHASED,
    acknowledged: Boolean = true,
    orderId: String = "GPA.3372-4150-8203-17209",
): Purchase = Purchase(
    """
    {
        "orderId": "$orderId",
        "packageName":"com.revenuecat.purchases_sample",
        "productIds":${JSONArray(productIds)},
        "purchaseTime":$purchaseTime,
        "purchaseState":${if (purchaseState == 2) 4 else 1},
        "purchaseToken":"$purchaseToken",
        "acknowledged":$acknowledged
    }
    """.trimIndent(),
    signature,
)

fun stubPurchaseHistoryRecord(
    productIds: List<String> = listOf("monthly_intro_pricing_one_week"),
    purchaseTime: Long = System.currentTimeMillis(),
    purchaseToken: String = "abcdefghijkcopgbomfinlko.AO-J1OxJixLsieYN08n9hV4qBsvqvQo6wXesyAClWs-t7KnYLCm3-" +
        "q6z8adcZnenbzqMHuMIqZ9kQ4KebT_Bge6KfZUhBt-0N0U0s71AEwFpzT7hrtErzdg",
    signature: String = "signature${System.currentTimeMillis()}",
): PurchaseHistoryRecord = PurchaseHistoryRecord(
    """
            {
                "productIds": ${JSONArray(productIds)},
                "purchaseTime": $purchaseTime,
                "purchaseToken": "$purchaseToken"
            }
    """.trimIndent(),
    signature,
)

fun stubStoreTransactionFromGooglePurchase(
    productIds: List<String>,
    purchaseTime: Long,
    purchaseToken: String = "abcdefghijipehfnbbnldmai.AO-J1OxqriTepvB7suzlIhxqPIveA0IHtX9amMedK0KK9CsO0S3Zk5H6gdwvV" +
        "7HzZIJeTzqkY4okyVk8XKTmK1WZKAKSNTKop4dgwSmFnLWsCxYbahUmADg",
): StoreTransaction {
    return stubGooglePurchase(
        productIds,
        purchaseTime,
        purchaseToken = purchaseToken,
    ).toStoreTransaction(ProductType.SUBS, null)
}

fun stubStoreTransactionFromPurchaseHistoryRecord(
    productIds: List<String>,
    purchaseTime: Long,
): StoreTransaction {
    return stubPurchaseHistoryRecord(
        productIds = productIds,
        purchaseTime = purchaseTime,
    ).toStoreTransaction(ProductType.SUBS)
}

fun BillingClient.mockQueryPurchaseHistory(
    result: BillingResult,
    history: List<PurchaseHistoryRecord>,
): Any {
    mockkStatic(QueryPurchaseHistoryParams::class)

    val mockBuilder = mockk<QueryPurchaseHistoryParams.Builder>(relaxed = true)
    every {
        QueryPurchaseHistoryParams.newBuilder()
    } returns mockBuilder

    every {
        mockBuilder.setProductType(any())
    } returns mockBuilder

    val params = mockk<QueryPurchaseHistoryParams>(relaxed = true)
    every {
        mockBuilder.build()
    } returns params

    val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()

    every {
        queryPurchaseHistoryAsync(
            params,
            capture(billingClientPurchaseHistoryListenerSlot),
        )
    } answers {
        billingClientPurchaseHistoryListenerSlot.captured.onPurchaseHistoryResponse(
            result,
            history,
        )
    }

    return mockBuilder
}

fun BillingClient.verifyQueryPurchaseHistoryCalledWithType(
    @BillingClient.ProductType googleType: String,
    builder: Any,
) {
    verify(exactly = 1) {
        (builder as QueryPurchaseHistoryParams.Builder).setProductType(googleType)
    }

    verify {
        queryPurchaseHistoryAsync(any<QueryPurchaseHistoryParams>(), any())
    }

    clearStaticMockk(QueryPurchasesParams::class)
}

fun BillingClient.mockQueryPurchasesAsync(
    subsResult: BillingResult,
    inAppResult: BillingResult,
    subPurchases: List<Purchase>,
    inAppPurchases: List<Purchase> = listOf(),
): Any {
    mockkStatic(QueryPurchasesParams::class)

    val mockBuilder = mockk<QueryPurchasesParams.Builder>(relaxed = true)
    every {
        QueryPurchasesParams.newBuilder()
    } returns mockBuilder

    val typeSlot = slot<String>()
    every {
        mockBuilder.setProductType(capture(typeSlot))
    } returns mockBuilder

    val params = mockk<QueryPurchasesParams>(relaxed = true)
    every {
        mockBuilder.build()
    } returns params

    val queryPurchasesListenerSlot = slot<PurchasesResponseListener>()

    every {
        queryPurchasesAsync(
            params,
            capture(queryPurchasesListenerSlot),
        )
    } answers {
        when (typeSlot.captured) {
            BillingClient.ProductType.SUBS -> {
                queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                    subsResult,
                    subPurchases,
                )
            }
            BillingClient.ProductType.INAPP -> {
                queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                    inAppResult,
                    inAppPurchases,
                )
            }
            else -> {
                fail("queryPurchasesAsync typeSlot not captured or captured unexpected type")
            }
        }
    }

    return mockBuilder
}
