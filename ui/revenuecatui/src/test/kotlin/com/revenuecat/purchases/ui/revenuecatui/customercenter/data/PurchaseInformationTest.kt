package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
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
import java.util.Locale
import kotlin.time.Duration.Companion.days

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
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = true
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.PLAY_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.PLAY_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            productIdentifier,
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
            locale = locale
        )

        assertThat(purchaseInformation.title).isEqualTo("Monthly Product")
        assertThat(purchaseInformation.durationTitle).isEqualTo("Month")
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.EARLIEST_RENEWAL)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Paid("$1.99"))

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.NEXT_BILLING_DATE)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.PLAY_STORE)
    }

    @Test
    fun `test PurchaseInformation with non-renewing Google subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"
        val isActive = true
        val willRenew = false
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.PLAY_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.PLAY_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            productIdentifier,
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
            locale = locale
        )

        assertThat(purchaseInformation.title).isEqualTo("Monthly Product")
        assertThat(purchaseInformation.durationTitle).isEqualTo("Month")
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.EARLIEST_EXPIRATION)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Paid("$1.99"))

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRES)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.PLAY_STORE)
    }

    @Test
    fun `test PurchaseInformation with expired Google subscription and entitlement`() {
        val expiresDate = oneDayAgo
        val expirationDateString = "2 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"
        val isActive = false
        val willRenew = false
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.PLAY_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.PLAY_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val storeProduct = TestStoreProduct(
            productIdentifier,
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
            locale = locale
        )

        assertThat(purchaseInformation.title).isEqualTo("Monthly Product")
        assertThat(purchaseInformation.durationTitle).isEqualTo("Month")
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.EXPIRED)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Paid("$1.99"))

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRED)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.PLAY_STORE)
    }

    @Test
    fun `test PurchaseInformation with active Apple subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = true
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.APP_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.APP_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.APPLE)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.NEXT_BILLING_DATE)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.APP_STORE)
    }

    @Test
    fun `test PurchaseInformation with non-renewing Apple subscription and entitlement`() {
        val expiresDate = oneDayFromNow
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = false
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.APP_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.APP_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.APPLE)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRES)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.APP_STORE)
    }

    @Test
    fun `test PurchaseInformation with expired Apple subscription and entitlement`() {
        val expiresDate = oneDayAgo
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = false
        val willRenew = false
        val productIdentifier = "test_product"
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expiresDate,
            store = Store.APP_STORE,
            productIdentifier = productIdentifier,
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.PURCHASED,
            jsonObject = mockk(),
            verification = VerificationResult.NOT_REQUESTED
        )
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = Store.APP_STORE,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.APPLE)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRED)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.APP_STORE)
    }

    @Test
    fun `test PurchaseInformation with promotional entitlement`() {
        val expiresDate = oneDayFromNow
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = false
        val productIdentifier = "rc_promo_pro_cat_yearly"
        val store = Store.PROMOTIONAL
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
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
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.PROMOTIONAL)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Free)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRES)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.PROMOTIONAL)
    }

    @Test
    fun `test PurchaseInformation with promotional lifetime entitlement`() {
        val expiresDate = null
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = false
        val productIdentifier = "rc_promo_pro_cat_yearly"
        val store = Store.PROMOTIONAL
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
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
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.PROMOTIONAL)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Free)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRES)
        assertThat(purchaseInformation.expirationOrRenewal.date).isEqualTo(ExpirationOrRenewal.Date.Never)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(Store.PROMOTIONAL)
    }

    @Test
    fun `test PurchaseInformation with stripe entitlement`() {
        val expiresDate = oneDayAgo
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = true
        val productIdentifier = "com.revenuecat.product"
        val store = Store.STRIPE
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
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
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.WEB)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.NEXT_BILLING_DATE)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(store)
    }

    @Test
    fun `test PurchaseInformation with non-renewing stripe entitlement`() {
        val expiresDate = oneDayAgo
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = true
        val willRenew = false
        val productIdentifier = "com.revenuecat.product"
        val store = Store.STRIPE
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
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
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.WEB)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRES)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(store)
    }

    @Test
    fun `test PurchaseInformation with expired stripe entitlement`() {
        val expiresDate = oneDayAgo
        val expirationDateString = "3 Oct 2063"
        every { dateFormatter.format(expiresDate, any()) } returns expirationDateString
        val purchaseDate = twoDaysAgo
        every { dateFormatter.format(purchaseDate, any()) } returns "1 Oct 2063"

        val isActive = false
        val willRenew = false
        val productIdentifier = "com.revenuecat.product"
        val store = Store.STRIPE
        val entitlementInfo = EntitlementInfo(
            identifier = "test_entitlement",
            isActive = isActive,
            willRenew = willRenew,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
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
        val transaction = TransactionDetails.Subscription(
            productIdentifier = productIdentifier,
            store = store,
            isActive = isActive,
            willRenew = willRenew,
            expiresDate = expiresDate
        )

        val purchaseInformation = PurchaseInformation(
            entitlementInfo = entitlementInfo,
            subscribedProduct = null,
            transaction = transaction,
            dateFormatter = dateFormatter,
            locale = locale
        )

        assertThat(purchaseInformation.title).isNull()
        assertThat(purchaseInformation.durationTitle).isNull()
        assertThat(purchaseInformation.explanation).isEqualTo(Explanation.WEB)
        assertThat(purchaseInformation.price).isEqualTo(PriceDetails.Unknown)

        assertThat(purchaseInformation.expirationOrRenewal).isNotNull
        assertThat(purchaseInformation.expirationOrRenewal!!.label)
            .isEqualTo(ExpirationOrRenewal.Label.EXPIRED)
        assertThat((purchaseInformation.expirationOrRenewal.date as ExpirationOrRenewal.Date.DateString).date)
            .isEqualTo(expirationDateString)
        assertThat(purchaseInformation.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchaseInformation.store).isEqualTo(store)
    }

}