package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.util.Iso8601Utils
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
        return JSONObject(
            "{\n" +
                "          \"id\": \"$id\",\n" +
                "          \"is_sandbox\": true,\n" +
                "          \"original_purchase_date\": \"$dateString\",\n" +
                "          \"purchase_date\": \"$dateString\",\n" +
                "          \"store\": \"app_store\"\n" +
                "        }"
        )
    }

    @Test
    fun `Can be created`() {
        val jsonObject = nonSubscriptionsJSONObject()
        val transaction = Transaction(productId, jsonObject)
        assertThat(transaction.transactionId).isEqualTo(id)
        assertThat(transaction.purchaseDate).isEqualTo(Iso8601Utils.parse(dateString))
        assertThat(transaction.productId).isEqualTo(productId)
    }

}