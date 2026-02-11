package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfoOriginalSource
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.HTTPResult
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
public class CustomerInfoFactoryTest {

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

    @Test
    fun `builds CustomerInfo with MAIN source from network response`() {
        val httpResult = HTTPResult(
            200,
            Responses.validFullPurchaserResponse,
            HTTPResult.Origin.BACKEND,
            null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(httpResult)
        assertThat(customerInfo.originalSource).isEqualTo(CustomerInfoOriginalSource.MAIN)
        assertThat(customerInfo.loadedFromCache).isFalse
    }

    @Test
    fun `builds CustomerInfo with LOAD_SHEDDER source when fortress header is true`() {
        val httpResult = HTTPResult(
            200,
            Responses.validFullPurchaserResponse,
            HTTPResult.Origin.BACKEND,
            null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(httpResult)
        assertThat(customerInfo.originalSource).isEqualTo(CustomerInfoOriginalSource.LOAD_SHEDDER)
        assertThat(customerInfo.loadedFromCache).isFalse
    }

    @Test
    fun `builds CustomerInfo with default MAIN source when fortress header is null`() {
        val httpResult = HTTPResult(
            200,
            Responses.validFullPurchaserResponse,
            HTTPResult.Origin.BACKEND,
            null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(httpResult)
        assertThat(customerInfo.originalSource).isEqualTo(CustomerInfoOriginalSource.MAIN)
        assertThat(customerInfo.loadedFromCache).isFalse
    }

    @Test
    fun `builds CustomerInfo with offline entitlements parameters`() {
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(
            JSONObject(Responses.validFullPurchaserResponse),
            overrideRequestDate = null,
            verificationResult = VerificationResult.NOT_REQUESTED,
            originalSource = CustomerInfoOriginalSource.OFFLINE_ENTITLEMENTS,
            loadedFromCache = false,
        )
        assertThat(customerInfo.originalSource).isEqualTo(CustomerInfoOriginalSource.OFFLINE_ENTITLEMENTS)
        assertThat(customerInfo.loadedFromCache).isFalse
    }
}
