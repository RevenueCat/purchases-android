package com.revenuecat.purchases.common.caching

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class LocalTransactionMetadataStoreTest {

    private lateinit var deviceCache: DeviceCache
    private lateinit var localTransactionMetadataStore: LocalTransactionMetadataStore

    private val purchaseToken = "test_purchase_token"
    private val purchaseToken2 = "test_purchase_token_2"
    private val appUserID = "test_user_id"

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
        sessionID = "session_id",
        revision = 1,
        displayMode = "full_screen",
        darkMode = false,
        localeIdentifier = "en_US",
        offeringId = "offering_id",
    )

    @Before
    fun setup() {
        deviceCache = mockk(relaxed = true)
        localTransactionMetadataStore = LocalTransactionMetadataStore(deviceCache)
    }

    // region cacheLocalTransactionMetadata

    @Test
    fun `cacheLocalTransactionMetadata saves data to device cache`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        verify(exactly = 1) {
            deviceCache.putString(
                "local_transaction_metadata",
                any()
            )
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata uses token hash as key`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        val retrieved = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(retrieved).isNotNull
    }

    @Test
    fun `cacheLocalTransactionMetadata skips if already cached`() {
        val existingMetadata = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to LocalTransactionMetadata.TransactionMetadata(
                    userID = appUserID,
                    token = purchaseToken,
                    receiptInfo = receiptInfo,
                    paywallPostReceiptData = null,
                    observerMode = false,
                )
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), existingMetadata)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        val newTransactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = "different_user",
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, newTransactionMetadata)

        // Should not call putString since it's already cached
        verify(exactly = 0) {
            deviceCache.putString(any(), any())
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata can cache multiple transactions`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        val transactionMetadata1 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val transactionMetadata2 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken2,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        // Cache first transaction
        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata1)

        // Mock the device cache to return the first transaction
        val firstCached = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata1
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), firstCached)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        // Cache second transaction
        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken2, transactionMetadata2)

        verify(atLeast = 2) {
            deviceCache.putString("local_transaction_metadata", any())
        }
    }

    @Test
    fun `cacheLocalTransactionMetadata handles paywall data`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        val paywallData = PaywallPostReceiptData(
            sessionID = "session_id",
            revision = 1,
            displayMode = "full_screen",
            darkMode = false,
            localeIdentifier = "en_US",
            offeringId = "offering_id",
        )

        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            observerMode = false,
        )

        localTransactionMetadataStore.cacheLocalTransactionMetadata(purchaseToken, transactionMetadata)

        val retrieved = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(retrieved?.paywallPostReceiptData).isEqualTo(paywallData)
    }

    // endregion

    // region getLocalTransactionMetadata

    @Test
    fun `getLocalTransactionMetadata returns null when no data cached`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        assertThat(result).isNull()
    }

    @Test
    fun `getLocalTransactionMetadata returns cached data`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        assertThat(result).isEqualTo(transactionMetadata)
    }

    @Test
    fun `getLocalTransactionMetadata returns null for wrong token`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        val result = localTransactionMetadataStore.getLocalTransactionMetadata("different_token")

        assertThat(result).isNull()
    }

    @Test
    fun `getLocalTransactionMetadata uses cached instance on subsequent calls`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        // First call
        localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        // Second call
        localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        // Should only call getJSONObjectOrNull once, using cached instance for second call
        verify(exactly = 1) {
            deviceCache.getJSONObjectOrNull("local_transaction_metadata")
        }
    }

    // endregion

    // region clearLocalTransactionMetadata

    @Test
    fun `clearLocalTransactionMetadata removes specific token`() {
        val transactionMetadata1 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val transactionMetadata2 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken2,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata1,
                purchaseToken2.sha1() to transactionMetadata2
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        verify(exactly = 1) {
            deviceCache.putString("local_transaction_metadata", any())
        }

        // Verify second token is still there
        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken2)
        assertThat(result).isNotNull
    }

    @Test
    fun `clearLocalTransactionMetadata removes all given tokens`() {
        val transactionMetadata1 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val transactionMetadata2 = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken2,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata1,
                purchaseToken2.sha1() to transactionMetadata2
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken, purchaseToken2))

        verify(exactly = 1) {
            deviceCache.putString("local_transaction_metadata", any())
        }

        assertThat(localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)).isNull()
        assertThat(localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken2)).isNull()
    }

    @Test
    fun `clearLocalTransactionMetadata does nothing when no data cached`() {
        every { deviceCache.getJSONObjectOrNull(any()) } returns null

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        verify(exactly = 0) {
            deviceCache.putString(any(), any())
        }
    }

    @Test
    fun `clearLocalTransactionMetadata does nothing when token not found`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("non_existent_token"))

        verify(exactly = 0) {
            deviceCache.putString(any(), any())
        }
    }

    @Test
    fun `clearLocalTransactionMetadata updates in-memory cache`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = null,
            observerMode = false,
        )

        val cachedData = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )
        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), cachedData)
        every { deviceCache.getJSONObjectOrNull("local_transaction_metadata") } returns JSONObject(jsonString)

        // Get the data to populate in-memory cache
        localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)

        // Clear the data
        localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(purchaseToken))

        // Verify it's gone from in-memory cache
        val result = localTransactionMetadataStore.getLocalTransactionMetadata(purchaseToken)
        assertThat(result).isNull()
    }

    // endregion

    // region Serialization

    @Test
    fun `LocalTransactionMetadata serializes correctly`() {
        val transactionMetadata = LocalTransactionMetadata.TransactionMetadata(
            userID = appUserID,
            token = purchaseToken,
            receiptInfo = receiptInfo,
            paywallPostReceiptData = paywallData,
            observerMode = false,
        )

        val localTransactionMetadata = LocalTransactionMetadata(
            purchaseDataByTokenHash = mapOf(
                purchaseToken.sha1() to transactionMetadata
            )
        )

        val jsonString = Json.encodeToString(LocalTransactionMetadata.serializer(), localTransactionMetadata)
        val deserialized = Json.decodeFromString(LocalTransactionMetadata.serializer(), jsonString)

        assertThat(deserialized).isEqualTo(localTransactionMetadata)

        // language=JSON
        val expectedJson = """
        {
           "purchase_data_by_token_hash":{
              "k3YkETIV3DZh8Pmq9NpLmd/WeYs=":{
                 "user_id":"test_user_id",
                 "token":"test_purchase_token",
                 "receipt_info":{
                    "productIDs":[
                       "product_id"
                    ],
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
                    "session_id":"session_id",
                    "revision":1,
                    "display_mode":"full_screen",
                    "dark_mode":false,
                    "locale":"en_US",
                    "offering_id":"offering_id"
                 },
                 "observer_mode":false
              }
           }
        }
        """.lines().joinToString("") { it.trim() }
        assertThat(jsonString).isEqualTo(expectedJson)
    }

    // endregion
}
