import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrenciesTest {

    // ----- Subscript Tests -----
    @Test
    fun `subscript returns correct virtual currency`() {
        val code = "COIN"
        val currency = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = code,
            serverDescription = "Coin currency"
        )
        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(code to currency),
            jsonObject = JSONObject()
        )

        val result = virtualCurrencies[code]
        assertEquals(currency, result)
    }

    @Test
    fun `subscript returns null for non-existent currency`() {
        val code = "COIN"
        val currency = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = code,
            serverDescription = "Coin currency"
        )
        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(code to currency),
            jsonObject = JSONObject()
        )

        assertNull(virtualCurrencies["NON_EXISTENT"])
    }
}