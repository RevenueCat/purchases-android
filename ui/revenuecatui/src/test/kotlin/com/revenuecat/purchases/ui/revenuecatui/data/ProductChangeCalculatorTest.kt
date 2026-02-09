package com.revenuecat.purchases.ui.revenuecatui.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.common.ProductChangeConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ProductChangeCalculatorTest {

    private lateinit var purchases: PurchasesType
    private lateinit var customerInfo: CustomerInfo
    private lateinit var calculator: ProductChangeCalculator

    private val defaultProductChangeConfig = ProductChangeConfig(
        upgradeReplacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
        downgradeReplacementMode = GoogleReplacementMode.DEFERRED,
    )

    @Before
    fun setUp() {
        purchases = mockk()
        customerInfo = mockk()
        calculator = ProductChangeCalculator(purchases)

        coEvery { purchases.awaitCustomerInfo() } returns customerInfo
    }

    @Test
    fun `returns null for non-subscription products`() = runTest {
        val lifetimePackage = createPackage(
            productId = "com.test.lifetime",
            period = null,
            priceMicros = 99_990_000,
            productType = ProductType.INAPP,
        )

        val result = calculator.calculateProductChangeInfo(
            lifetimePackage,
            defaultProductChangeConfig,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when user has no active subscriptions`() = runTest {
        every { customerInfo.subscriptionsByProductIdentifier } returns emptyMap()

        val packageToPurchase = createSubscriptionPackage(
            productId = "com.test.subscription:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            packageToPurchase,
            defaultProductChangeConfig,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when active subscription is not from Play Store`() = runTest {
        val appStoreSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.basic",
            store = Store.APP_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.basic" to appStoreSubscription,
        )

        val packageToPurchase = createSubscriptionPackage(
            productId = "com.test.subscription.premium:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 19_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            packageToPurchase,
            defaultProductChangeConfig,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when purchasing same product with different base plan`() = runTest {
        val activeSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription" to activeSubscription,
        )

        val sameProductDifferentBasePlan = createSubscriptionPackage(
            productId = "com.test.subscription:annual_plan",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            priceMicros = 99_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            sameProductDifferentBasePlan,
            defaultProductChangeConfig,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `detects upgrade - higher price per month in production`() = runTest {
        val currentSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.basic",
            productPlanIdentifier = "monthly",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.basic" to currentSubscription,
        )

        val oldProduct = createStoreProduct(
            productId = "com.test.subscription.basic",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 4_990_000,
        )
        coEvery { purchases.awaitGetProduct("com.test.subscription.basic", "monthly") } returns oldProduct

        val premiumPackage = createSubscriptionPackage(
            productId = "com.test.subscription.premium:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            premiumPackage,
            defaultProductChangeConfig,
        )

        assertThat(result).isNotNull
        assertThat(result!!.oldProductId).isEqualTo("com.test.subscription.basic")
        assertThat(result.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
    }

    @Test
    fun `detects downgrade - lower price per month in production`() = runTest {
        val currentSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.premium",
            productPlanIdentifier = "monthly",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.premium" to currentSubscription,
        )

        val oldProduct = createStoreProduct(
            productId = "com.test.subscription.premium",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )
        coEvery { purchases.awaitGetProduct("com.test.subscription.premium", "monthly") } returns oldProduct

        val basicPackage = createSubscriptionPackage(
            productId = "com.test.subscription.basic:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 4_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            basicPackage,
            defaultProductChangeConfig,
        )

        assertThat(result).isNotNull
        assertThat(result!!.oldProductId).isEqualTo("com.test.subscription.premium")
        assertThat(result.replacementMode).isEqualTo(GoogleReplacementMode.DEFERRED)
    }

    @Test
    fun `uses sandbox timing for upgrade detection in sandbox mode`() = runTest {
        val currentSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.basic",
            productPlanIdentifier = "monthly",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = true,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.basic" to currentSubscription,
        )

        val oldProduct = createStoreProduct(
            productId = "com.test.subscription.basic",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 500_000,
        )
        coEvery { purchases.awaitGetProduct("com.test.subscription.basic", "monthly") } returns oldProduct

        val premiumPackage = createSubscriptionPackage(
            productId = "com.test.subscription.premium:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 1_000_000,
        )

        val result = calculator.calculateProductChangeInfo(
            premiumPackage,
            defaultProductChangeConfig,
        )

        assertThat(result).isNotNull
        assertThat(result!!.oldProductId).isEqualTo("com.test.subscription.basic")
        assertThat(result.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
    }

    @Test
    fun `uses custom replacement modes from config`() = runTest {
        val currentSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.basic",
            productPlanIdentifier = "monthly",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.basic" to currentSubscription,
        )

        val oldProduct = createStoreProduct(
            productId = "com.test.subscription.basic",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 4_990_000,
        )
        coEvery { purchases.awaitGetProduct("com.test.subscription.basic", "monthly") } returns oldProduct

        val premiumPackage = createSubscriptionPackage(
            productId = "com.test.subscription.premium:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )

        val customConfig = ProductChangeConfig(
            upgradeReplacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
            downgradeReplacementMode = GoogleReplacementMode.WITH_TIME_PRORATION,
        )

        val result = calculator.calculateProductChangeInfo(
            premiumPackage,
            customConfig,
        )

        assertThat(result).isNotNull
        assertThat(result!!.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_FULL_PRICE)
    }

    @Test
    fun `defaults to downgrade when old product cannot be fetched`() = runTest {
        val currentSubscription = createSubscriptionInfo(
            productIdentifier = "com.test.subscription.basic",
            productPlanIdentifier = "monthly",
            store = Store.PLAY_STORE,
            isActive = true,
            isSandbox = false,
        )
        every { customerInfo.subscriptionsByProductIdentifier } returns mapOf(
            "com.test.subscription.basic" to currentSubscription,
        )

        coEvery { purchases.awaitGetProduct("com.test.subscription.basic", "monthly") } returns null

        val premiumPackage = createSubscriptionPackage(
            productId = "com.test.subscription.premium:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            premiumPackage,
            defaultProductChangeConfig,
        )

        assertThat(result).isNotNull
        assertThat(result!!.replacementMode).isEqualTo(GoogleReplacementMode.DEFERRED)
    }

    @Test
    fun `returns null when exception is thrown`() = runTest {
        coEvery { purchases.awaitCustomerInfo() } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.NetworkError, "Network error"),
        )

        val packageToPurchase = createSubscriptionPackage(
            productId = "com.test.subscription:monthly",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            priceMicros = 9_990_000,
        )

        val result = calculator.calculateProductChangeInfo(
            packageToPurchase,
            defaultProductChangeConfig,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parseProductIdentifier extracts subscription id and base plan`() {
        val (subscriptionId, basePlanId) = ProductChangeCalculator.parseProductIdentifier(
            "com.test.subscription:monthly_plan",
        )

        assertThat(subscriptionId).isEqualTo("com.test.subscription")
        assertThat(basePlanId).isEqualTo("monthly_plan")
    }

    @Test
    fun `parseProductIdentifier handles missing base plan`() {
        val (subscriptionId, basePlanId) = ProductChangeCalculator.parseProductIdentifier(
            "com.test.subscription",
        )

        assertThat(subscriptionId).isEqualTo("com.test.subscription")
        assertThat(basePlanId).isNull()
    }

    @Test
    fun `getSandboxRenewalMinutes returns correct values`() {
        assertThat(ProductChangeCalculator.getSandboxRenewalMinutes(
            Period(1, Period.Unit.WEEK, "P1W"),
        )).isEqualTo(5L)

        assertThat(ProductChangeCalculator.getSandboxRenewalMinutes(
            Period(1, Period.Unit.MONTH, "P1M"),
        )).isEqualTo(5L)

        assertThat(ProductChangeCalculator.getSandboxRenewalMinutes(
            Period(3, Period.Unit.MONTH, "P3M"),
        )).isEqualTo(10L)

        assertThat(ProductChangeCalculator.getSandboxRenewalMinutes(
            Period(6, Period.Unit.MONTH, "P6M"),
        )).isEqualTo(15L)

        assertThat(ProductChangeCalculator.getSandboxRenewalMinutes(
            Period(1, Period.Unit.YEAR, "P1Y"),
        )).isEqualTo(30L)
    }

    @Test
    fun `getNormalizedPrice uses pricePerMonth for production`() {
        val yearlyProduct = createStoreProduct(
            productId = "com.test.subscription",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            priceMicros = 59_990_000,
        )

        val normalizedPrice = with(ProductChangeCalculator) {
            yearlyProduct.getNormalizedPrice(isSandbox = false)
        }

        val expectedPricePerMonth = yearlyProduct.pricePerMonth()?.amountMicros
        assertThat(normalizedPrice).isEqualTo(expectedPricePerMonth)
    }

    @Test
    fun `getNormalizedPrice uses sandbox timing for sandbox`() {
        val yearlyProduct = createStoreProduct(
            productId = "com.test.subscription",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            priceMicros = 30_000_000,
        )

        val normalizedPrice = with(ProductChangeCalculator) {
            yearlyProduct.getNormalizedPrice(isSandbox = true)
        }

        assertThat(normalizedPrice).isEqualTo(30_000_000 / 30L)
    }

    @Test
    fun `getNormalizedPrice returns null for products without period`() {
        val lifetimeProduct = createStoreProduct(
            productId = "com.test.lifetime",
            period = null,
            priceMicros = 99_990_000,
        )

        val normalizedPrice = with(ProductChangeCalculator) {
            lifetimeProduct.getNormalizedPrice(isSandbox = false)
        }

        assertThat(normalizedPrice).isNull()
    }

    private fun createSubscriptionPackage(
        productId: String,
        period: Period,
        priceMicros: Long,
    ): Package {
        return createPackage(
            productId = productId,
            period = period,
            priceMicros = priceMicros,
            productType = ProductType.SUBS,
        )
    }

    private fun createPackage(
        productId: String,
        period: Period?,
        priceMicros: Long,
        productType: ProductType,
    ): Package {
        val product = if (productType == ProductType.SUBS) {
            TestStoreProduct(
                id = productId,
                name = productId,
                title = productId,
                description = productId,
                price = Price(amountMicros = priceMicros, currencyCode = "USD", formatted = "$$priceMicros"),
                period = period,
            )
        } else {
            TestStoreProduct(
                id = productId,
                name = productId,
                title = productId,
                description = productId,
                price = Price(amountMicros = priceMicros, currencyCode = "USD", formatted = "$$priceMicros"),
                period = null,
            )
        }
        return Package(
            packageType = PackageType.CUSTOM,
            identifier = productId,
            product = product,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "test_offering"),
        )
    }

    private fun createStoreProduct(
        productId: String,
        period: Period?,
        priceMicros: Long,
    ): TestStoreProduct {
        return TestStoreProduct(
            id = productId,
            name = productId,
            title = productId,
            description = productId,
            price = Price(amountMicros = priceMicros, currencyCode = "USD", formatted = "$$priceMicros"),
            period = period,
        )
    }

    private fun createSubscriptionInfo(
        productIdentifier: String,
        productPlanIdentifier: String? = null,
        store: Store,
        isActive: Boolean,
        isSandbox: Boolean,
    ): SubscriptionInfo {
        val now = Date()
        return SubscriptionInfo(
            productIdentifier = productIdentifier,
            purchaseDate = now,
            originalPurchaseDate = null,
            expiresDate = if (isActive) Date(System.currentTimeMillis() + 86400000) else Date(0),
            store = store,
            unsubscribeDetectedAt = null,
            isSandbox = isSandbox,
            billingIssuesDetectedAt = null,
            gracePeriodExpiresDate = null,
            ownershipType = OwnershipType.PURCHASED,
            periodType = PeriodType.NORMAL,
            refundedAt = null,
            storeTransactionId = null,
            autoResumeDate = null,
            displayName = null,
            price = null,
            productPlanIdentifier = productPlanIdentifier,
            managementURL = null,
            requestDate = now,
        )
    }
}
