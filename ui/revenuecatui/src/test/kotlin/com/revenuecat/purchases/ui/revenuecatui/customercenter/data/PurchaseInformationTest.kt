package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.TransactionDetails
import com.revenuecat.purchases.ui.revenuecatui.helpers.ago
import com.revenuecat.purchases.ui.revenuecatui.helpers.fromNow
import com.revenuecat.purchases.ui.revenuecatui.utils.DateFormatter
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.days

private const val MANAGEMENT_URL = "https://play.google.com/store/account/subscriptions"

@RunWith(AndroidJUnit4::class)
class PurchaseInformationTest {

    private val oneDayAgo = 1.days.ago()
    private val twoDaysAgo = 2.days.ago()
    private val oneDayFromNow = 1.days.fromNow()

    private val dateFormatter = mockk<DateFormatter>()
    private val locale = Locale.US

    @Test
    fun `test PurchaseInformation with active Google subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = storeProduct,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale,
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = false,
            renewalDate = "3 Oct 2063",
            expirationDate = null
        )
    }

    @Test
    fun `test PurchaseInformation with non-renewing Google subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = storeProduct,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with expired Google subscription and entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "2 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = false,
            willRenew = false,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = false,
            willRenew = false,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = storeProduct,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "2 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with active Apple subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = true,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "test_product",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = false,
            renewalDate = "3 Oct 2063",
            expirationDate = null
        )
    }

    @Test
    fun `test PurchaseInformation with non-renewing Apple subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "test_product",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with expired Apple subscription and entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = false,
            willRenew = false,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = false,
            willRenew = false,
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "test_product",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with promotional entitlement`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_yearly",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_yearly",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "rc_promo_pro_cat_yearly",
            price = PriceDetails.Free,
            store = Store.PROMOTIONAL,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with promotional lifetime entitlement`() {
        val expiresDate = null
        setupDateFormatter(expiresDate, "")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_yearly",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_yearly",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "rc_promo_pro_cat_yearly",
            price = PriceDetails.Free,
            store = Store.PROMOTIONAL,
            product = null,
            isLifetime = true,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = null
        )
    }

    @Test
    fun `test PurchaseInformation with stripe entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = true,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = false,
            renewalDate = "3 Oct 2063",
            expirationDate = null
        )
    }

    @Test
    fun `test PurchaseInformation with non-renewing stripe entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with expired stripe entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = false,
            willRenew = false,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = false,
            willRenew = false,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with paddle entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = true,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = false,
            renewalDate = "3 Oct 2063",
            expirationDate = null
        )
    }

    @Test
    fun `test PurchaseInformation with non-renewing paddle entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with expired paddle entitlement`() {
        val expiresDate = oneDayAgo
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = false,
            willRenew = false,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = false,
            willRenew = false,
            store = Store.PADDLE,
            productIdentifier = "com.revenuecat.product",
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "com.revenuecat.product",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = true,
            renewalDate = null,
            expirationDate = "3 Oct 2063"
        )
    }

    @Test
    fun `test PurchaseInformation with trial subscription`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expirationDate = expiresDate
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate,
            isTrial = true
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = storeProduct,
            transaction = transaction,
            managementURL = Uri.parse(MANAGEMENT_URL),
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isLifetime = false,
            isActive = true,
            isTrial = true,
            isCancelled = false,
            renewalDate = "3 Oct 2063",
            expirationDate = null
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun assertPurchaseInformation(
        purchaseInformation: PurchaseInformation,
        title: String?,
        price: PriceDetails,
        product: StoreProduct?,
        store: Store,
        isLifetime: Boolean,
        isActive: Boolean = true,
        isTrial: Boolean = false,
        isCancelled: Boolean = false,
        renewalDate: String? = null,
        expirationDate: String? = null,
    ) {
        assertThat(purchaseInformation.title).isEqualTo(title)
        assertThat(purchaseInformation.pricePaid).isEqualTo(price)
        assertThat(purchaseInformation.isLifetime).isEqualTo(isLifetime)
        assertThat(purchaseInformation.product).isEqualTo(product)
        assertThat(purchaseInformation.store).isEqualTo(store)
        assertThat(purchaseInformation.isActive).isEqualTo(isActive)
        assertThat(purchaseInformation.isTrial).isEqualTo(isTrial)
        assertThat(purchaseInformation.isCancelled).isEqualTo(isCancelled)
        assertThat(purchaseInformation.renewalDate).isEqualTo(renewalDate)
        assertThat(purchaseInformation.expirationDate).isEqualTo(expirationDate)
    }

    private fun createEntitlementInfo(
        isActive: Boolean,
        willRenew: Boolean,
        store: Store,
        productIdentifier: String,
        expirationDate: Date?
    ): EntitlementInfo {
        return EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = twoDaysAgo,
            originalPurchaseDate = twoDaysAgo,
            expirationDate = expirationDate,
            store = store,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
    }

    private fun createTransactionDetails(
        isActive: Boolean,
        willRenew: Boolean,
        store: Store,
        productIdentifier: String,
        expiresDate: Date?,
        productPlanIdentifier: String? = null,
        isTrial: Boolean = false,
    ): TransactionDetails.Subscription {
        return TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate,
            productPlanIdentifier = productPlanIdentifier,
            isTrial = isTrial,
        )
    }

    private fun setupDateFormatter(expiresDate: Date?, expirationDateString: String) {
        if (expiresDate != null) {
            every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        }
        every { dateFormatter.format(twoDaysAgo, any()) } returns "1 Oct 2063"
    }
}
