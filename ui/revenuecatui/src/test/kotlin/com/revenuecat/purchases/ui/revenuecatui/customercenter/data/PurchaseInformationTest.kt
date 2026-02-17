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
    private val localization = CustomerCenterConfigTestData.customerCenterData().localization

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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization,
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = ExpirationOrRenewal.Renewal("3 Oct 2063"),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = true,
            isExpired = true,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("2 Oct 2063"),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = ExpirationOrRenewal.Renewal("3 Oct 2063"),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.APP_STORE,
            product = null,
            isSubscription = true,
            isExpired = true,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
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
            expirationDate = expiresDate,
            ownershipType = OwnershipType.UNKNOWN
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_yearly",
            expiresDate = expiresDate,
            managementURL = null,
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization,
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "test_entitlement",
            price = PriceDetails.Free,
            store = Store.PROMOTIONAL,
            product = null,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
            managementURL = null,
        )
    }

    @Test
    fun `test PurchaseInformation with promotional lifetime entitlement`() {
        val expiresDate = 1_000_000_000.days.fromNow()
        setupDateFormatter(expiresDate, "3 Oct 2222")

        val entitlementInfo = createEntitlementInfo(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_lifetime",
            expirationDate = expiresDate,
            ownershipType = OwnershipType.UNKNOWN
        )
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = false,
            store = Store.PROMOTIONAL,
            productIdentifier = "rc_promo_pro_cat_lifetime",
            expiresDate = expiresDate,
            managementURL = null,
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "test_entitlement",
            price = PriceDetails.Free,
            store = Store.PROMOTIONAL,
            product = null,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2222"),
            managementURL = null,
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = ExpirationOrRenewal.Renewal("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.STRIPE,
            product = null,
            isSubscription = true,
            isExpired = true,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = ExpirationOrRenewal.Renewal("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isSubscription = true,
            isExpired = false,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Subscription",
            price = PriceDetails.Unknown,
            store = Store.PADDLE,
            product = null,
            isSubscription = true,
            isExpired = true,
            isTrial = false,
            isCancelled = true,
            expirationOrRenewal = ExpirationOrRenewal.Expiration("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
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
            expirationDate = expiresDate,
            ownershipType = OwnershipType.PURCHASED
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
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Monthly Product",
            price = PriceDetails.Paid("$1.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = true,
            isExpired = false,
            isTrial = true,
            isCancelled = false,
            expirationOrRenewal = ExpirationOrRenewal.Renewal("3 Oct 2063"),
            managementURL = Uri.parse(MANAGEMENT_URL),
        )
    }

    @Test
    fun `test PurchaseInformation with no entitlement and no subscribed product shows user-friendly fallback`() {
        val subscriptionTransaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.technical.id",
            expiresDate = null
        )
        val subscriptionPurchaseInfo = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = null,
            transaction = subscriptionTransaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertThat(subscriptionPurchaseInfo.title).isEqualTo("Subscription")

        val nonSubscriptionTransaction = createNonSubscriptionTransactionDetails(
            store = Store.STRIPE,
            productIdentifier = "com.revenuecat.technical.id"
        )
        val nonSubscriptionPurchaseInfo = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = null,
            transaction = nonSubscriptionTransaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertThat(nonSubscriptionPurchaseInfo.title).isEqualTo("One time purchase")
    }

    @Test
    fun `test non-sandbox transaction with zero price shows as free`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")
        
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate,
            price = Price("$0.00", 0L, "USD"),
            isSandbox = false
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$9.99", 9_990_000, "USD"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = storeProduct,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertThat(purchaseInformation.pricePaid).isEqualTo(PriceDetails.Free)
    }

    @Test
    fun `test non-sandbox transaction with price shows transaction price`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")
        
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate,
            price = Price("$4.99", 4_990_000, "USD"),
            isSandbox = false
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$9.99", 9_990_000, "USD"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = storeProduct,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertThat(purchaseInformation.pricePaid).isEqualTo(PriceDetails.Paid("$4.99"))
    }

    @Test
    fun `test sandbox transaction with non-zero price shows transaction price`() {
        val expiresDate = oneDayFromNow
        setupDateFormatter(expiresDate, "3 Oct 2063")
        
        val transaction = createTransactionDetails(
            isActive = true,
            willRenew = true,
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            expiresDate = expiresDate,
            price = Price("$4.99", 4_990_000, "USD"),
            isSandbox = true
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Monthly Product",
            "description",
            Price("$9.99", 9_990_000, "USD"),
            Period(1, Period.Unit.MONTH, "P1M")
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = storeProduct,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertThat(purchaseInformation.pricePaid).isEqualTo(PriceDetails.Paid("$4.99"))
    }

    @Test
    fun `test one-time purchase with product shows product information`() {
        val transaction = createNonSubscriptionTransactionDetails(
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            price = Price("$9.99", 9_990_000, "USD"),
            isSandbox = false
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "One-time Product",
            "description",
            Price("$9.99", 9_990_000, "USD"),
            null
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = storeProduct,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "One-time Product",
            price = PriceDetails.Paid("$9.99"),
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = null,
            managementURL = null
        )
    }

    @Test
    fun `test one-time purchase without product shows fallback title`() {
        val transaction = createNonSubscriptionTransactionDetails(
            store = Store.APP_STORE,
            productIdentifier = "test_product",
            price = Price("$4.99", 4_990_000, "USD"),
            isSandbox = false
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "One time purchase",
            price = PriceDetails.Paid("$4.99"),
            store = Store.APP_STORE,
            product = null,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = null,
            managementURL = null
        )
    }

    @Test
    fun `test one-time purchase with zero price shows as free`() {
        val transaction = createNonSubscriptionTransactionDetails(
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            price = Price("$0.00", 0L, "USD"),
            isSandbox = false
        )

        val storeProduct = TestStoreProduct(
            "test_product",
            "name",
            "Free Product",
            "description",
            Price("$0.00", 0L, "USD"),
            null
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = storeProduct,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "Free Product",
            price = PriceDetails.Free,
            store = Store.PLAY_STORE,
            product = storeProduct,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = null,
            managementURL = null
        )
    }

    @Test
    fun `test one-time purchase without price shows unknown`() {
        val transaction = createNonSubscriptionTransactionDetails(
            store = Store.PLAY_STORE,
            productIdentifier = "test_product",
            price = null,
            isSandbox = false
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = null,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale,
            localization = localization
        )

        assertPurchaseInformation(
            purchaseInformation,
            title = "One time purchase",
            price = PriceDetails.Unknown,
            store = Store.PLAY_STORE,
            product = null,
            isSubscription = false,
            isExpired = false,
            isTrial = false,
            isCancelled = false,
            expirationOrRenewal = null,
            managementURL = null
        )
    }

    @Test
    fun `test lifetime purchase shows as lifetime`() {
        assertThat(CustomerCenterConfigTestData.purchaseInformationLifetime.isLifetime).isTrue()
    }

    @Test
    fun `test subscription purchase shows as not lifetime`() {
        assertThat(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing.isLifetime).isFalse()
    }

    private fun assertPurchaseInformation(
        purchaseInformation: PurchaseInformation,
        title: String?,
        price: PriceDetails,
        product: StoreProduct?,
        store: Store,
        isSubscription: Boolean,
        isExpired: Boolean = false,
        isTrial: Boolean = false,
        isCancelled: Boolean = false,
        expirationOrRenewal: ExpirationOrRenewal? = null,
        managementURL: Uri? = Uri.parse(MANAGEMENT_URL)
    ) {
        assertThat(purchaseInformation.title).isEqualTo(title)
        assertThat(purchaseInformation.pricePaid).isEqualTo(price)
        assertThat(purchaseInformation.isSubscription).isEqualTo(isSubscription)
        assertThat(purchaseInformation.product).isEqualTo(product)
        assertThat(purchaseInformation.store).isEqualTo(store)
        assertThat(purchaseInformation.isExpired).isEqualTo(isExpired)
        assertThat(purchaseInformation.isTrial).isEqualTo(isTrial)
        assertThat(purchaseInformation.isCancelled).isEqualTo(isCancelled)
        assertThat(purchaseInformation.expirationOrRenewal).isEqualTo(expirationOrRenewal)
        assertThat(purchaseInformation.managementURL).isEqualTo(managementURL)
    }

    private fun createEntitlementInfo(
        isActive: Boolean,
        willRenew: Boolean,
        store: Store,
        productIdentifier: String,
        expirationDate: Date?,
        ownershipType: OwnershipType = OwnershipType.PURCHASED,
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
            ownershipType = ownershipType,
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
        managementURL: Uri? = Uri.parse(MANAGEMENT_URL),
        price: Price? = null,
        isSandbox: Boolean = false
    ): TransactionDetails.Subscription {
        return TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate,
            productPlanIdentifier = productPlanIdentifier,
            isTrial = isTrial,
            managementURL = managementURL,
            price = price,
            isSandbox = isSandbox
        )
    }

    private fun createNonSubscriptionTransactionDetails(
        store: Store,
        productIdentifier: String,
        price: Price? = null,
        isSandbox: Boolean = false
    ): TransactionDetails.NonSubscription {
        return TransactionDetails.NonSubscription(
            productIdentifier = productIdentifier,
            store = store,
            price = price,
            isSandbox = isSandbox
        )
    }

    private fun setupDateFormatter(expiresDate: Date?, expirationDateString: String) {
        if (expiresDate != null) {
            every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        }
        every { dateFormatter.format(twoDaysAgo, any()) } returns "1 Oct 2063"
    }
}
