package com.revenuecat.purchases

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PurchaseIntegrationTest {

    private val testTimeout = 5.seconds

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    private lateinit var mockBillingAbstract: BillingAbstract

    @Before
    fun setup() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            mockBillingAbstract = mockk(relaxed = true)

            Purchases.configure(
                PurchasesConfiguration.Builder(it, "REVENUECAT_API_KEY")
                    .appUserID("integrationTest")
                    .build(),
                mockBillingAbstract
            )
        }
    }

    @After
    fun tearDown() {
        Purchases.sharedInstance.close()
    }

    @Test
    fun sdkCanBeConfigured() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            assertThat(Purchases.sharedInstance.appUserID).isNotNull()
        }
    }

    @Test
    fun customerInfoCanBeFetched() {
        val lock = CountDownLatch(1)

        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            Purchases.sharedInstance.getCustomerInfoWith({
                fail("should be success. Error: ${it.message}")
            }) {
                lock.countDown()
            }
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun customerInfoCanBeFetchedFromBackendAndThenGottenFromCache() {
        val lock = CountDownLatch(1)

        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            Purchases.sharedInstance.getCustomerInfoWith(
                CacheFetchPolicy.FETCH_CURRENT,
                onError = {
                    fail("fetching from backend should be success. Error: ${it.message}")
                },
                onSuccess = { fetchedCustomerInfo ->
                    Purchases.sharedInstance.getCustomerInfoWith(
                        CacheFetchPolicy.CACHE_ONLY,
                        onError = {
                            fail("fetching from cache should be success. Error: ${it.message}")
                        },
                        onSuccess = { cachedCustomerInfo ->
                            assertThat(cachedCustomerInfo).isEqualTo(fetchedCustomerInfo)
                            lock.countDown()
                        }
                    )
                }
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Suppress("LongMethod")
    @Test
    fun canFetchOfferings() {
        val lock = CountDownLatch(1)

        val storeProduct = StoreProduct(
            sku = "monthly_intro_pricing_one_week",
            type = ProductType.SUBS,
            price = "€5.49",
            priceAmountMicros = 5490000,
            priceCurrencyCode = "EUR",
            originalPrice = "€5.49",
            originalPriceAmountMicros = 5490000,
            title = "Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)",
            description = "Monthly Product Intro Pricing One Week",
            subscriptionPeriod = "P1M",
            freeTrialPeriod = "P1W",
            introductoryPrice = null,
            introductoryPriceAmountMicros = 0,
            introductoryPricePeriod = null,
            introductoryPriceCycles = 0,
            iconUrl = "",
            originalJson = JSONObject("{" +
                "\"productId\":\"monthly_intro_pricing_one_week\"," +
                "\"type\":\"subs\"," +
                "\"title\":\"Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)\"," +
                "\"name\":\"Monthly Product Intro Pricing One Week\"," +
                "\"description\":\"Monthly Product Intro Pricing One Week\"," +
                "\"price\":\"€5.49\"," +
                "\"price_amount_micros\":5490000," +
                "\"price_currency_code\":\"EUR\"," +
                "\"skuDetailsToken\":\"test-token\"," +
                "\"subscriptionPeriod\":\"P1M\"," +
                "\"freeTrialPeriod\":\"P1W\"" +
                "}")
        )
        val subsReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            mockBillingAbstract.querySkuDetailsAsync(ProductType.SUBS, any(), capture(subsReceiveCallbackSlot), any())
        } answers {
            subsReceiveCallbackSlot.captured.invoke(listOf(storeProduct))
        }

        val inappReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            mockBillingAbstract.querySkuDetailsAsync(ProductType.INAPP, any(), capture(inappReceiveCallbackSlot), any())
        } answers {
            inappReceiveCallbackSlot.captured.invoke(emptyList())
        }

        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error -> fail("Get offerings should be successful. Error: ${error.message}") },
                onSuccess = { offerings ->
                    assertThat(offerings.current).isNotNull
                    assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
                    assertThat(offerings.current?.monthly?.product?.sku).isEqualTo("monthly_intro_pricing_one_week")

                    lock.countDown()
                }
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }
}
