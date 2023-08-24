package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.PurchasesErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingResponseCodeTest {

    @Test
    fun `There are no new BillingResponseCodes`() {
        val allPossibleBillingResponseCodes = BillingClient.BillingResponseCode::class.java.declaredFields
        assertThat(allPossibleBillingResponseCodes)
            .withFailMessage("It looks like there are new BillingResponseCodes, " +
                "make sure to update the error conversions functions to include the new errors.")
            .hasSize(13)
    }

    @Test
    fun `All BillingResponseCodes are handled when converting to PurchasesError`() {
        val allPossibleBillingResponseCodes = BillingClient.BillingResponseCode::class.java.declaredFields
        allPossibleBillingResponseCodes.forEach { field ->
            val value = field.getInt(field)
            val billingResponseToPurchasesError = value.billingResponseToPurchasesError("")
            if (value != BillingClient.BillingResponseCode.OK) {
                assertThat(billingResponseToPurchasesError.code)
                    .withFailMessage("BillingClient.BillingResponseCode.%s is unhandled", field.name)
                    .isNotEqualTo(PurchasesErrorCode.UnknownError)
            } else {
                assertThat(billingResponseToPurchasesError.code).isEqualTo(PurchasesErrorCode.UnknownError)
            }
        }
    }

    @Test
    fun `BillingResponseCodeNames are correctly generated`() {
        val allResponseCodesToName = setOf(
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT to "SERVICE_TIMEOUT",
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED to "FEATURE_NOT_SUPPORTED",
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED to "SERVICE_DISCONNECTED",
            BillingClient.BillingResponseCode.OK to "OK",
            BillingClient.BillingResponseCode.USER_CANCELED to "USER_CANCELED",
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE to "SERVICE_UNAVAILABLE",
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE to "BILLING_UNAVAILABLE",
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE to "ITEM_UNAVAILABLE",
            BillingClient.BillingResponseCode.DEVELOPER_ERROR to "DEVELOPER_ERROR",
            BillingClient.BillingResponseCode.ERROR to "ERROR",
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED to "ITEM_ALREADY_OWNED",
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED to "ITEM_NOT_OWNED",
            BillingClient.BillingResponseCode.NETWORK_ERROR to "NETWORK_ERROR"
        )

        val allPossibleBillingResponseCodes = BillingClient.BillingResponseCode::class.java.declaredFields
        assertThat(allPossibleBillingResponseCodes)
            .withFailMessage("Looks like the test is not handling all BillingResponseCodes")
            .hasSameSizeAs(allResponseCodesToName)

        allResponseCodesToName.forEach { (billingResponseCode, name) ->
            assertThat(billingResponseCode.getBillingResponseCodeName()).isEqualTo(name)
        }
    }
}
