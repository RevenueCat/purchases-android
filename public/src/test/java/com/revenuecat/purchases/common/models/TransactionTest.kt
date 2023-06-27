package com.revenuecat.purchases.common.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.utils.Iso8601UtilsDupe
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionTest {
    private val id = "72c26cc69c"
    private val productId = "productID"
    private val dateString = "1990-08-30T02:40:36Z"

    private fun nonSubscriptionsJSONObject(): JSONObject {
        return JSONObject("""
            {
              "id": "$id",
              "is_sandbox": true,
              "original_purchase_date": "$dateString",
              "purchase_date": "$dateString",
              "store": "app_store"
            }
        """)
    }

    @Test
    fun `Can be created`() {
        val jsonObject = nonSubscriptionsJSONObject()
        val transaction = Transaction(productId, jsonObject)
        assertThat(transaction.transactionIdentifier).isEqualTo(id)
        assertThat(transaction.purchaseDate).isEqualTo(Iso8601UtilsDupe.parse(dateString))
        assertThat(transaction.productIdentifier).isEqualTo(productId)
    }

}
