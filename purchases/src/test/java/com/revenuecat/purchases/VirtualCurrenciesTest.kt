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

    // ----- fromJSON Tests -----
    @Test
    fun `fromJson with empty JSON returns empty map`() {
        val json = JSONObject()
        val result = VirtualCurrencies.fromJson(json)
        assertTrue(result.all.isEmpty())
    }

    @Test
    fun `fromJson with single virtual currency parses correctly`() {
        val code = "COIN"
        val balance = 100
        val name = "Coin"
        val description = "Virtual currency"

        val currencyJson = JSONObject().apply {
            put("balance", balance)
            put("name", name)
            put("code", code)
            put("description", description)
        }
        val json = JSONObject().apply {
            put("virtual_currencies", JSONObject().apply {
                put(code, currencyJson)
            })
        }

        val result = VirtualCurrencies.fromJson(json)
        
        assertEquals(1, result.all.size)
        val currency = result.all[code]
        assertEquals(balance, currency?.balance)
        assertEquals(name, currency?.name)
        assertEquals(code, currency?.code)
        assertEquals(description, currency?.serverDescription)
    }

    @Test
    fun `fromJson with multiple virtual currencies parses correctly`() {
        val coinsCode = "COIN"
        val coinsBalance = 100
        val coinsName = "Coins"
        val coinsDescription = "Coin currency"

        val gemsCode = "GEM"
        val gemsBalance = 50
        val gemsName = "Gems"
        val gemsDescription = "Gems currency"

        val coinsJson = JSONObject().apply {
            put("balance", coinsBalance)
            put("name", coinsName)
            put("code", coinsCode)
            put("description", coinsDescription)
        }
        val gemsJson = JSONObject().apply {
            put("balance", gemsBalance)
            put("name", gemsName)
            put("code", gemsCode)
            put("description", gemsDescription)
        }
        val json = JSONObject().apply {
            put("virtual_currencies", JSONObject().apply {
                put(coinsCode, coinsJson)
                put(gemsCode, gemsJson)
            })
        }

        val result = VirtualCurrencies.fromJson(json)
        
        assertEquals(2, result.all.size)
        
        val coins = result.all[coinsCode]
        assertEquals(coinsBalance, coins?.balance)
        assertEquals(coinsName, coins?.name)
        assertEquals(coinsCode, coins?.code)
        assertEquals(coinsDescription, coins?.serverDescription)
        
        val gems = result.all[gemsCode]
        assertEquals(gemsBalance, gems?.balance)
        assertEquals(gemsName, gems?.name)
        assertEquals(gemsCode, gems?.code)
        assertEquals(gemsDescription, gems?.serverDescription)
    }

    @Test
    fun `fromJson with null description parses correctly`() {
        val code = "COIN"
        val balance = 100
        val name = "Coins"

        val currencyJson = JSONObject().apply {
            put("balance", balance)
            put("name", name)
            put("code", code)
            // description is omitted to test null case
        }
        val json = JSONObject().apply {
            put("virtual_currencies", JSONObject().apply {
                put(code, currencyJson)
            })
        }

        val result = VirtualCurrencies.fromJson(json)
        
        assertEquals(1, result.all.size)
        val currency = result.all[code]
        assertEquals(balance, currency?.balance)
        assertEquals(name, currency?.name)
        assertEquals(code, currency?.code)
        assertEquals(null, currency?.serverDescription)
    }

    @Test
    fun `fromJson parses correctly from JSON string`() {
        val code = "COIN"
        val balance = 100
        val name = "Coins"
        val description = "Coin currency"

        val jsonString = """
            {
                "virtual_currencies": {
                    "$code": {
                        "balance": $balance,
                        "name": "$name",
                        "code": "$code",
                        "description": "$description"
                    }
                }
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = VirtualCurrencies.fromJson(json)
        
        assertEquals(1, result.all.size)
        val currency = result.all[code]
        assertEquals(balance, currency?.balance)
        assertEquals(name, currency?.name)
        assertEquals(code, currency?.code)
        assertEquals(description, currency?.serverDescription)
    }

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
        val virtualCurrencies = VirtualCurrencies(all = mapOf(code to currency))
        
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
        val virtualCurrencies = VirtualCurrencies(all = mapOf(code to currency))
        
        assertNull(virtualCurrencies["NON_EXISTENT"])
    }

    // ----- withNonZeroBalance Tests -----
    @Test
    fun `withNonZeroBalance returns only currencies with positive balance`() {
        val coin = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = "COIN",
            serverDescription = "Coin currency"
        )
        val gem = VirtualCurrency(
            balance = 0,
            name = "Gems",
            code = "GEM",
            serverDescription = "Gem currency"
        )
        val diamond = VirtualCurrency(
            balance = 50,
            name = "Diamonds",
            code = "DMND",
            serverDescription = "Diamond currency"
        )

        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(
                "COIN" to coin,
                "GEM" to gem,
                "DMND" to diamond
            )
        )

        val nonZeroBalances = virtualCurrencies.withNonZeroBalance
        assertEquals(2, nonZeroBalances.size)
        assertEquals(coin, nonZeroBalances["COIN"])
        assertEquals(diamond, nonZeroBalances["DMND"])
        assertNull(nonZeroBalances["GEM"])
    }

    @Test
    fun `withNonZeroBalance returns empty map when all balances are zero`() {
        val coin = VirtualCurrency(
            balance = 0,
            name = "Coins",
            code = "COIN",
            serverDescription = "Coin currency"
        )
        val gem = VirtualCurrency(
            balance = 0,
            name = "Gems",
            code = "GEM",
            serverDescription = "Gem currency"
        )

        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(
                "COIN" to coin,
                "GEM" to gem
            )
        )

        val nonZeroBalances = virtualCurrencies.withNonZeroBalance
        assertTrue(nonZeroBalances.isEmpty())
    }

    fun `withNonZeroBalance returns empty map when there are no virtual currencies`() {
        val virtualCurrencies = VirtualCurrencies(all = mapOf())
        val nonZeroBalances = virtualCurrencies.withNonZeroBalance
        assertTrue(nonZeroBalances.isEmpty())
    }

    // ----- withZeroBalance Tests -----
    @Test
    fun `withZeroBalance returns only currencies with zero balance`() {
        val coin = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = "COIN",
            serverDescription = "Coin currency"
        )
        val gem = VirtualCurrency(
            balance = 0,
            name = "Gems",
            code = "GEM",
            serverDescription = "Gem currency"
        )
        val diamond = VirtualCurrency(
            balance = 50,
            name = "Diamonds",
            code = "DMND",
            serverDescription = "Diamond currency"
        )

        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(
                "COIN" to coin,
                "GEM" to gem,
                "DMND" to diamond
            )
        )

        val zeroBalances = virtualCurrencies.withZeroBalance
        assertEquals(1, zeroBalances.size)
        assertEquals(gem, zeroBalances["GEM"])
        assertNull(zeroBalances["COIN"])
        assertNull(zeroBalances["DMND"])
    }

    @Test
    fun `withZeroBalance returns empty map when all balances are non-zero`() {
        val coin = VirtualCurrency(
            balance = 100,
            name = "Coins",
            code = "COIN",
            serverDescription = "Coin currency"
        )
        val gem = VirtualCurrency(
            balance = 50,
            name = "Gems",
            code = "GEM",
            serverDescription = "Gem currency"
        )

        val virtualCurrencies = VirtualCurrencies(
            all = mapOf(
                "COIN" to coin,
                "GEM" to gem
            )
        )

        val zeroBalances = virtualCurrencies.withZeroBalance
        assertTrue(zeroBalances.isEmpty())
    }

    fun `withZeroBalance returns empty map when there are no virtual currencies`() {
        val virtualCurrencies = VirtualCurrencies(all = mapOf())
        val nonZeroBalances = virtualCurrencies.withZeroBalance
        assertTrue(nonZeroBalances.isEmpty())
    }
}