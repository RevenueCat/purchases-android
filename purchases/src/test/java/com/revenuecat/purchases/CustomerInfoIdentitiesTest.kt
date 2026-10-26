package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.createCustomerInfo
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerInfoIdentitiesTest {

    @Test
    fun `no user object means not all identities are anonymous`() {
        assertThat(createCustomerInfo(Responses.validEmptyPurchaserResponse).allIdentitiesAreAnonymous).isFalse()
    }

    @Test
    fun `an empty amr means not all identities are anonymous`() {
        assertThat(customerInfoWithAmr().allIdentitiesAreAnonymous).isFalse()
    }

    @Test
    fun `a user without an amr means not all identities are anonymous`() {
        val json = JSONObject(Responses.validEmptyPurchaserResponse).put("user", JSONObject().put("id", "user"))

        assertThat(createCustomerInfo(json).allIdentitiesAreAnonymous).isFalse()
    }

    @Test
    fun `an amr of only anonymous entries means all identities are anonymous`() {
        assertThat(customerInfoWithAmr("anonymous", "anonymous").allIdentitiesAreAnonymous).isTrue()
    }

    @Test
    fun `any non-anonymous amr entry means not all identities are anonymous`() {
        assertThat(customerInfoWithAmr("anonymous", "google").allIdentitiesAreAnonymous).isFalse()
    }

    @Test
    fun `an unrecognized amr entry is treated as not anonymous`() {
        assertThat(customerInfoWithAmr("future-provider").allIdentitiesAreAnonymous).isFalse()
    }

    @Test
    fun `the user object survives a raw data round trip`() {
        val roundTripped = createCustomerInfo(customerInfoWithAmr("anonymous").rawData.toString())

        assertThat(roundTripped.allIdentitiesAreAnonymous).isTrue()
    }

    private fun customerInfoWithAmr(vararg amr: String): CustomerInfo =
        createCustomerInfo(
            JSONObject(Responses.validEmptyPurchaserResponse)
                .put("user", JSONObject().put("id", "user").put("amr", JSONArray(amr.toList()))),
        )
}
