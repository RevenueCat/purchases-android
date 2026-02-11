package com.revenuecat.purchases.common.caching

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class LocalTransactionMetadataStoreTest {

    private val json = JsonTools.json

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var localTransactionMetadataStore: LocalTransactionMetadataStore

    private val apiKey = "test_api_key"
    private val purchaseToken = "test_purchase_token"
    private val purchaseToken2 = "test_purchase_token_2"

    private val receiptInfo = ReceiptInfo(
        productIDs = listOf("product_id"),
        presentedOfferingContext = PresentedOfferingContext("offering_id"),
        price = 4.99,
        formattedPrice = "$4.99",
        currency = "USD",
        period = null,
        pricingPhases = null,
        replacementMode = null,
        platformProductIds = emptyList(),
    )

    private val paywallData = PaywallPostReceiptData(
        paywallID = "paywall_id",
        sessionID = "session_id",
        revision = 1,
        displayMode = "full_screen",
        darkMode = false,
        localeIdentifier = "en_US",
        offeringId = "offering_id",
    )

    @Before
    public fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } just Runs
        every { sharedPreferences.all } returns emptyMap()

        localTransactionMetadataStore = LocalTransactionMetadataStore(context, apiKey, sharedPreferences)
    }

    // region cacheLocalTransactionMetadata

    @Test
    fun `cacheLocalTransactionMetadata saves data to shared preferences`() {
        every { sharedPreferences.contains(any()) } returns false
        every { sharedPreferences.getString(any(), any()) } returns null

        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        val expectedKey = "local_transaction_metadata_${purchaseToken.sha1()}"
        verify(exactly = 1) {
            editor.putString(expectedKey, any())
        }
        verify(exactly = 1) {
            editor.apply()
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata uses token hash as key`() {
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.contains(key) } returns false

        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonSlot = slot<String>()
        every { editor.putString(key, capture(jsonSlot)) } returns editor

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        // Mock SharedPreferences to return what was written
        every { sharedPreferences.getString(key, null) } answers { jsonSlot.captured }

        val retrieved = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(retrieved).isNotNull
        assertThat(retrieved).isEqualTo(transactionMetadata)
    }

    @Test
    fun `cacheLocalTransactionMetadata skips if already exists in SharedPreferences`() {
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.contains(key) } returns true

        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        // Should not call putString since it already exists in SharedPreferences
        verify(exactly = 0) {
            editor.putString(any(), any())
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata can cache multiple transactions`() {
        every { sharedPreferences.contains(any()) } returns false
        every { sharedPreferences.getString(any(), any()) } returns null

        val transactionMetadata1 = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val transactionMetadata2 = LocalTransactionMetadata(
            token = purchaseToken2,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata1)
        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken2, transactionMetadata2)

        verify(exactly = 1) {
            editor.putString("local_transaction_metadata_${purchaseToken.sha1()}", any())
        }
        verify(exactly = 1) {
            editor.putString("local_transaction_metadata_${purchaseToken2.sha1()}", any())
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata handles paywall data`() {
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.contains(key) } returns false

        val paywallData = PaywallPostReceiptData(
            paywallID = "paywall_id",
            sessionID = "session_id",
            revision = 1,
            displayMode = "full_screen",
            darkMode = false,
            localeIdentifier = "en_US",
            offeringId = "offering_id",
        )

        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonSlot = slot<String>()
        every { editor.putString(key, capture(jsonSlot)) } returns editor

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        // Mock SharedPreferences to return what was written
        every { sharedPreferences.getString(key, null) } answers { jsonSlot.captured }

        val retrieved = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(retrieved?.paywallPostReceiptData).isEqualTo(paywallData)
    }

    // endregion

    // region getLocalTransactionMetadata

    @Test
    fun `getLocalTransactionMetadata returns null when no data cached`() {
        every { sharedPreferences.getString(any(), any()) } returns null

        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        assertThat(result).isNull()
    }

    @Test
    fun `getLocalTransactionMetadata returns cached data`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.getString(key, null) } returns jsonString

        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        assertThat(result).isEqualTo(transactionMetadata)
    }

    @Test
    fun `getLocalTransactionMetadata returns null for wrong token`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.getString(key, null) } returns jsonString
        every { sharedPreferences.getString(not(eq(key)), any()) } returns null

        val result = localTransactionMetadataStore.getLocalTransactionMetadata("different_token")

        assertThat(result).isNull()
    }

    @Test
    fun `getLocalTransactionMetadata reads from SharedPreferences on each call`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.getString(key, null) } returns jsonString

        // First call
        val result1 = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        // Second call
        val result2 = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        // Both calls should return the same data
        assertThat(result1).isEqualTo(transactionMetadata)
        assertThat(result2).isEqualTo(transactionMetadata)

        // SharedPreferences is called each time (no in-memory cache)
        verify(exactly = 2) {
            sharedPreferences.getString(key, null)
        }
    }

    @Test
    fun `getLocalTransactionMetadata clears corrupted data`() {
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.getString(key, null) } returns "invalid json"

        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        assertThat(result).isNull()
        verify(exactly = 1) {
            editor.remove(key)
        }
    }

    // endregion

    // region getAllLocalTransactionMetadata

    @Test
    fun `getAllLocalTransactionMetadata returns empty list when no data cached`() {
        every { sharedPreferences.all } returns emptyMap()

        val result = localTransactionMetadataStore.getAllLocalTransactionMetadata()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getAllLocalTransactionMetadata returns single item when one transaction cached`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)

        every { sharedPreferences.all } returns mapOf(key to jsonString)
        every { sharedPreferences.getString(key, null) } returns jsonString

        val result = localTransactionMetadataStore.getAllLocalTransactionMetadata()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(transactionMetadata)
    }

    @Test
    fun `getAllLocalTransactionMetadata returns all items when multiple transactions cached`() {
        val transactionMetadata1 = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val transactionMetadata2 = LocalTransactionMetadata(
            token = purchaseToken2,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val key1 = "local_transaction_metadata_${purchaseToken.sha1()}"
        val key2 = "local_transaction_metadata_${purchaseToken2.sha1()}"
        val jsonString1 = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata1)
        val jsonString2 = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata2)

        every { sharedPreferences.all } returns mapOf(key1 to jsonString1, key2 to jsonString2)
        every { sharedPreferences.getString(key1, null) } returns jsonString1
        every { sharedPreferences.getString(key2, null) } returns jsonString2

        val result = localTransactionMetadataStore.getAllLocalTransactionMetadata()

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyInAnyOrder(transactionMetadata1, transactionMetadata2)
    }

    @Test
    fun `getAllLocalTransactionMetadata ignores keys without prefix`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)

        every { sharedPreferences.all } returns mapOf(
            key to jsonString,
            "some_other_key" to "some_value",
            "another_key" to "another_value"
        )
        every { sharedPreferences.getString(key, null) } returns jsonString

        val result = localTransactionMetadataStore.getAllLocalTransactionMetadata()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(transactionMetadata)
    }

    @Test
    fun `getAllLocalTransactionMetadata skips corrupted entries`() {
        val validMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val key1 = "local_transaction_metadata_${purchaseToken.sha1()}"
        val key2 = "local_transaction_metadata_${purchaseToken2.sha1()}"
        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), validMetadata)

        every { sharedPreferences.all } returns mapOf(
            key1 to jsonString,
            key2 to "invalid json"
        )
        every { sharedPreferences.getString(key1, null) } returns jsonString
        every { sharedPreferences.getString(key2, null) } returns "invalid json"

        val result = localTransactionMetadataStore.getAllLocalTransactionMetadata()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(validMetadata)
        verify(exactly = 1) {
            editor.remove(key2)
        }
    }

    // endregion

    // region clearLocalTransactionMetadata

    @Test
    fun `clearLocalTransactionMetadata removes specific token`() {
        val key1 = "local_transaction_metadata_${purchaseToken.sha1()}"
        val key2 = "local_transaction_metadata_${purchaseToken2.sha1()}"

        every { sharedPreferences.contains(key1) } returns true
        every { sharedPreferences.contains(key2) } returns true

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        verify(exactly = 1) {
            editor.remove(key1)
        }
        verify(exactly = 0) {
            editor.remove(key2)
        }
        verify(exactly = 1) {
            editor.apply()
        }
    }

    @Test
    fun `clearLocalTransactionMetadata removes all given tokens`() {
        val key1 = "local_transaction_metadata_${purchaseToken.sha1()}"
        val key2 = "local_transaction_metadata_${purchaseToken2.sha1()}"

        every { sharedPreferences.contains(key1) } returns true
        every { sharedPreferences.contains(key2) } returns true

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken, purchaseToken2))

        verify(exactly = 1) {
            editor.remove(key1)
        }
        verify(exactly = 1) {
            editor.remove(key2)
        }
        verify(exactly = 1) {
            editor.apply()
        }
    }

    @Test
    fun `clearLocalTransactionMetadata does nothing when no data cached`() {
        every { sharedPreferences.contains(any()) } returns false

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        verify(exactly = 0) {
            editor.remove(any())
        }
    }

    @Test
    fun `clearLocalTransactionMetadata does nothing when token not found`() {
        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        every { sharedPreferences.contains(key) } returns true
        every { sharedPreferences.contains(not(eq(key))) } returns false

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("non_existent_token"))

        verify(exactly = 0) {
            editor.remove(any())
        }
    }

    @Test
    fun `clearLocalTransactionMetadata removes data from SharedPreferences`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val key = "local_transaction_metadata_${purchaseToken.sha1()}"
        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)
        every { sharedPreferences.getString(key, null) } returns jsonString
        every { sharedPreferences.contains(key) } returns true

        // Verify data exists before clearing
        localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        // Clear the data
        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        // Mock that it's been removed from SharedPreferences
        every { sharedPreferences.getString(key, null) } returns null

        // Verify it's gone
        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(result).isNull()
    }

    @Test
    fun `clearLocalTransactionMetadata does nothing for empty set`() {
        localTransactionMetadataStore.clearLocalTransactionMetadata(emptySet())

        verify(exactly = 0) {
            editor.remove(any())
        }
        verify(exactly = 0) {
            editor.apply()
        }
    }

    // endregion

    // region Serialization

    @Test
    fun `LocalTransactionMetadata serializes correctly`() {
        val transactionMetadata = LocalTransactionMetadata(
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )

        val jsonString = json.encodeToString(LocalTransactionMetadata.serializer(), transactionMetadata)
        val deserialized = json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)

        // language=json
        val expectedJson = """
            {
                "token":"$purchaseToken",
                "receipt_info":{
                    "productIDs":["product_id"],
                    "presentedOfferingContext":{
                        "offeringIdentifier":"offering_id",
                        "placementIdentifier":null,
                        "targetingContext":null
                    },
                    "price":4.99,
                    "formattedPrice":"$4.99",
                    "currency":"USD"
                },
                "paywall_data":{
                    "paywall_id":"paywall_id",
                    "session_id":"session_id",
                    "revision":1,
                    "display_mode":"full_screen",
                    "dark_mode":false,
                    "locale":"en_US",
                    "offering_id":"offering_id"
                },
                "purchases_are_completed_by":"REVENUECAT"
            }
        """.trimIndent().lines().joinToString("") { it.trim() }
        assertThat(jsonString).isEqualTo(expectedJson)

        assertThat(deserialized).isEqualTo(transactionMetadata)
    }

    // endregion
}
