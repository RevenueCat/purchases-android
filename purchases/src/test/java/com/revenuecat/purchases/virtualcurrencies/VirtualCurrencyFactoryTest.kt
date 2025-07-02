package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrencyFactoryTest {
    private val defaultVirtualCurrencyWithDescription = VirtualCurrencyFactory.buildVirtualCurrency(
        JSONObject(Responses.validVirtualCurrencyResponseWithDescription)
    )

    private val defaultVirtualCurrencyWithoutDescription = VirtualCurrencyFactory.buildVirtualCurrency(
        JSONObject(Responses.validVirtualCurrencyResponseWithoutDescription)
    )

    private val defaultVirtualCurrencyWithNegativeBalance = VirtualCurrencyFactory.buildVirtualCurrency(
        JSONObject(Responses.validVirtualCurrencyResponseWithNegativeBalance)
    )

    @Test
    fun `correctly parses virtual currency with description`() {
        assertThat(defaultVirtualCurrencyWithDescription).isNotNull
        assertThat(defaultVirtualCurrencyWithDescription.name).isEqualTo("Coin")
        assertThat(defaultVirtualCurrencyWithDescription.balance).isEqualTo(1)
        assertThat(defaultVirtualCurrencyWithDescription.serverDescription).isEqualTo("It's a coin")
        assertThat(defaultVirtualCurrencyWithDescription.code).isEqualTo("COIN")
    }

    @Test
    fun `correctly parses virtual currency without description`() {
        assertThat(defaultVirtualCurrencyWithoutDescription).isNotNull
        assertThat(defaultVirtualCurrencyWithoutDescription.name).isEqualTo("RC Coin")
        assertThat(defaultVirtualCurrencyWithoutDescription.balance).isEqualTo(0)
        assertThat(defaultVirtualCurrencyWithoutDescription.serverDescription).isNull()
        assertThat(defaultVirtualCurrencyWithoutDescription.code).isEqualTo("RC_COIN")
    }

    @Test
    fun `correctly parses virtual currency with negative balance`() {
        assertThat(defaultVirtualCurrencyWithNegativeBalance).isNotNull
        assertThat(defaultVirtualCurrencyWithNegativeBalance.name).isEqualTo("Negative")
        assertThat(defaultVirtualCurrencyWithNegativeBalance.balance).isEqualTo(-1)
        assertThat(defaultVirtualCurrencyWithNegativeBalance.serverDescription).isNull()
        assertThat(defaultVirtualCurrencyWithNegativeBalance.code).isEqualTo("NEGATIVE")
    }

    @Test
    fun `throws JSONException for invalid JSON`() {
        assertThatThrownBy {
            VirtualCurrencyFactory.buildVirtualCurrency(
                JSONObject("{}")
            )
        }.isInstanceOf(JSONException::class.java)

        assertThatThrownBy {
            VirtualCurrencyFactory.buildVirtualCurrency(
                JSONObject("asdf")
            )
        }.isInstanceOf(JSONException::class.java)
    }
}
