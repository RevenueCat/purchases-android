package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class CustomerInfoFactoryTest {

    private val defaultCustomerInfo = CustomerInfoFactory.buildCustomerInfo(
        JSONObject(Responses.validFullPurchaserResponse),
        overrideRequestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED
    )

    @Test
    fun `parses non subscription transactions correctly`() {
        val firstNonSubscriptionTransaction = Transaction(
            transactionIdentifier = "72c26cc69c",
            revenuecatId = "72c26cc69c",
            productIdentifier = "100_coins_pack",
            productId = "100_coins_pack",
            purchaseDate = Iso8601Utils.parse("1990-08-30T02:40:36Z"),
            storeTransactionId = null,
            store = Store.APP_STORE,
            displayName = "100 Coins",
            isSandbox = true,
            originalPurchaseDate = Iso8601Utils.parse("1990-08-30T02:40:36Z"),
            price = Price(formatted="$0.99", amountMicros=990000, currencyCode="USD"),
        )
        assertThat(defaultCustomerInfo.nonSubscriptionTransactions.size).isEqualTo(5)
        assertThat(defaultCustomerInfo.nonSubscriptionTransactions.first()).isEqualTo(firstNonSubscriptionTransaction)
    }

    @Test
    fun `assigns active entitlements correctly`() {
        assertThat(defaultCustomerInfo.entitlements.active.keys).isEqualTo(setOf("pro", "forever_pro"))
    }

    @Test
    fun `assigns default schema version correctly`() {
        assertThat(defaultCustomerInfo.schemaVersion).isEqualTo(3)
    }

    @Test
    fun `assigns default request date correctly`() {
        assertThat(defaultCustomerInfo.requestDate).isEqualTo(Date(1565951442000))
    }

    @Test
    fun `assigns default verification result correctly`() {
        assertThat(defaultCustomerInfo.entitlements.verification).isEqualTo(VerificationResult.NOT_REQUESTED)
    }
}
