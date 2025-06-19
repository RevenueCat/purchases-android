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
class VirtualCurrenciesFactoryTest {
    private val defaultVirtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
        JSONObject(Responses.validFullVirtualCurrenciesResponse)
    )

    private val emptyVirtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
        JSONObject(Responses.validEmptyVirtualCurrenciesResponse)
    )

    @Test
    fun `correctly parses virtual currencies`() {
        assertThat(defaultVirtualCurrencies.all.size).isEqualTo(2)

        val coinVC: VirtualCurrency? = defaultVirtualCurrencies["COIN"]
        assertThat(coinVC).isNotNull
        assertThat(coinVC?.name).isEqualTo("Coin")
        assertThat(coinVC?.balance).isEqualTo(1)
        assertThat(coinVC?.serverDescription).isEqualTo("It's a coin")
        assertThat(coinVC?.code).isEqualTo("COIN")

        val rcCoinVC: VirtualCurrency? = defaultVirtualCurrencies["RC_COIN"]
        assertThat(rcCoinVC).isNotNull
        assertThat(rcCoinVC?.name).isEqualTo("RC Coin")
        assertThat(rcCoinVC?.balance).isEqualTo(0)
        assertThat(rcCoinVC?.serverDescription).isNull()
        assertThat(rcCoinVC?.code).isEqualTo("RC_COIN")

        val nonExistentVC: VirtualCurrency? = defaultVirtualCurrencies["asdf"]
        assertThat(nonExistentVC).isNull()
    }

    @Test
    fun `correctly parses empty virtual currencies`() {
        assertThat(emptyVirtualCurrencies.all.size).isEqualTo(0)
    }

    @Test
    fun `throws JSONException for invalid JSON`() {
        assertThatThrownBy {
            VirtualCurrenciesFactory.buildVirtualCurrencies(
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
