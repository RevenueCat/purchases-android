package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementInfo
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
            expiresDate = expiresDate
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
            durationTitle = "Month",
            explanation = Explanation.EARLIEST_RENEWAL,
            price = PriceDetails.Paid("$1.99"),
            expirationLabel = ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
            expirationDateString = "3 Oct 2063",
            store = Store.PLAY_STORE,
            product = storeProduct
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
            expiresDate = expiresDate
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
            durationTitle = "Month",
            explanation = Explanation.EARLIEST_EXPIRATION,
            price = PriceDetails.Paid("$1.99"),
            expirationLabel = ExpirationOrRenewal.Label.EXPIRES,
            expirationDateString = "3 Oct 2063",
            store = Store.PLAY_STORE,
            product = storeProduct
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
            expiresDate = expiresDate
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
            durationTitle = "Month",
            explanation = Explanation.EXPIRED,
            price = PriceDetails.Paid("$1.99"),
            expirationLabel = ExpirationOrRenewal.Label.EXPIRED,
            expirationDateString = "2 Oct 2063",
            store = Store.PLAY_STORE,
            product = storeProduct,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.APPLE,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
            expirationDateString = "3 Oct 2063",
            store = Store.APP_STORE,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.APPLE,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRES,
            expirationDateString = "3 Oct 2063",
            store = Store.APP_STORE,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.APPLE,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRED,
            expirationDateString = "3 Oct 2063",
            store = Store.APP_STORE,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.PROMOTIONAL,
            price = PriceDetails.Free,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRES,
            expirationDateString = "3 Oct 2063",
            store = Store.PROMOTIONAL,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.PROMOTIONAL,
            price = PriceDetails.Free,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRES,
            expirationDateString = "",
            store = Store.PROMOTIONAL,
            product = null,
        )
        assertThat(purchaseInformation.expirationOrRenewal!!.date).isEqualTo(ExpirationOrRenewal.Date.Never)
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.WEB,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
            expirationDateString = "3 Oct 2063",
            store = Store.STRIPE,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.WEB,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRES,
            expirationDateString = "3 Oct 2063",
            store = Store.STRIPE,
            product = null,
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
            expiresDate = expiresDate
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
            title = null,
            durationTitle = null,
            explanation = Explanation.WEB,
            price = PriceDetails.Unknown,
            expirationLabel = ExpirationOrRenewal.Label.EXPIRED,
            expirationDateString = "3 Oct 2063",
            product = null,
            store = Store.STRIPE
        )
    }

    private fun assertPurchaseInformation(
        purchaseInformation: PurchaseInformation,
        title: String?,
        durationTitle: String?,
        explanation: Explanation,
        price: PriceDetails,
        expirationLabel: ExpirationOrRenewal.Label,
        expirationDateString: String,
        product: StoreProduct?,
        store: Store
    ) {
        assertThat(purchaseInformation.title).isEqualTo(title)
        assertThat(purchaseInformation.durationTitle).isEqualTo(durationTitle)
        assertThat(purchaseInformation.explanation).isEqualTo(explanation)
        assertThat(purchaseInformation.price).isEqualTo(price)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label).isEqualTo(expirationLabel)
        if (expirationDateString.isNotEmpty()) {
            assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
                .isEqualTo(expirationDateString)
        }
        assertThat(purchaseInformation.product).isEqualTo(product)
        assertThat(purchaseInformation.store).isEqualTo(store)
    }

    private fun createEntitlementInfo(
        isActive: Boolean,
        willRenew: Boolean,
        store: Store,
        productIdentifier: String,
        expiresDate: Date?
    ): EntitlementInfo {
        return EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = twoDaysAgo,
            originalPurchaseDate = twoDaysAgo,
            expirationDate = expiresDate,
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
        expiresDate: Date?
    ): TransactionDetails.Subscription {
        return TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )
    }

    private fun setupDateFormatter(expiresDate: Date?, expirationDateString: String) {
        if (expiresDate != null) {
            every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        }
        every { dateFormatter.format(twoDaysAgo, any()) } returns "1 Oct 2063"
    }
}