package com.revenuecat.purchases.paywalls

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class PaywallPresentedCacheTest {

    private lateinit var cache: PaywallPresentedCache

    @Before
    public fun setup() {
        cache = PaywallPresentedCache()
    }

    // region receiveEvent tests

    @Test
    fun `receiveEvent caches PURCHASE_INITIATED event`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1")

        cache.receiveEvent(event)

        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()
    }

    @Test
    fun `receiveEvent clears cache on CANCEL event`() {
        val purchaseInitiatedEvent = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1")
        cache.receiveEvent(purchaseInitiatedEvent)
        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()

        val cancelEvent = createPaywallEvent(PaywallEventType.CANCEL, "product_1")
        cache.receiveEvent(cancelEvent)

        assertThat(cache.hasCachedPurchaseInitiatedData()).isFalse()
    }

    @Test
    fun `receiveEvent clears cache on PURCHASE_ERROR event`() {
        val purchaseInitiatedEvent = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1")
        cache.receiveEvent(purchaseInitiatedEvent)
        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()

        val errorEvent = createPaywallEvent(PaywallEventType.PURCHASE_ERROR, "product_1")
        cache.receiveEvent(errorEvent)

        assertThat(cache.hasCachedPurchaseInitiatedData()).isFalse()
    }

    @Test
    fun `receiveEvent ignores other event types`() {
        val event = createPaywallEvent(PaywallEventType.IMPRESSION, "product_1")

        cache.receiveEvent(event)

        assertThat(cache.hasCachedPurchaseInitiatedData()).isFalse()
    }

    @Test
    fun `receiveEvent replaces previously cached PURCHASE_INITIATED event`() {
        val firstEvent = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 1000L)
        cache.receiveEvent(firstEvent)

        val secondEvent = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_2", timestamp = 2000L)
        cache.receiveEvent(secondEvent)

        val retrievedEvent = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_2"),
            3000L
        )

        assertThat(retrievedEvent).isEqualTo(secondEvent)
    }

    // endregion

    // region getAndRemovePurchaseInitiatedEventIfNeeded tests

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns null when no cached event`() {
        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            1000L,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns null when product ID doesn't match`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 1000L)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_2"),
            2000L,
        )

        assertThat(result).isNull()
        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns null when purchase timestamp is before event date`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 2000L)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            1000L,
        )

        assertThat(result).isNull()
        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns null when purchase timestamp is null`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 1000L)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            null,
        )

        assertThat(result).isNull()
        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns event when all conditions are met`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 1000L)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            2000L,
        )

        assertThat(result).isEqualTo(event)
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded removes event from cache after retrieval`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 1000L)
        cache.receiveEvent(event)

        cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            2000L,
        )

        assertThat(cache.hasCachedPurchaseInitiatedData()).isFalse()
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns event when product ID is in list`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_2", timestamp = 1000L)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1", "product_2", "product_3"),
            2000L,
        )

        assertThat(result).isEqualTo(event)
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded returns event when purchase timestamp equals event date`() {
        val timestamp = 1000L
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = timestamp)
        cache.receiveEvent(event)

        val result = cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            timestamp
        )

        assertThat(result).isEqualTo(event)
    }

    @Test
    fun `getAndRemovePurchaseInitiatedEventIfNeeded does not remove event when conditions not met`() {
        val event = createPaywallEvent(PaywallEventType.PURCHASE_INITIATED, "product_1", timestamp = 2000L)
        cache.receiveEvent(event)

        cache.getAndRemovePurchaseInitiatedEventIfNeeded(
            listOf("product_1"),
            1000L // Purchase before event
        )

        assertThat(cache.hasCachedPurchaseInitiatedData()).isTrue()
    }

    // endregion

    // region Helper methods

    private fun createPaywallEvent(
        type: PaywallEventType,
        productId: String,
        timestamp: Long = 1000L
    ): PaywallEvent {
        return PaywallEvent(
            creationData = PaywallEvent.CreationData(
                id = UUID.randomUUID(),
                date = Date(timestamp)
            ),
            data = PaywallEvent.Data(
                paywallIdentifier = "test_paywall",
                presentedOfferingContext = PresentedOfferingContext("test_offering"),
                paywallRevision = 1,
                sessionIdentifier = UUID.randomUUID(),
                displayMode = "full_screen",
                localeIdentifier = "en_US",
                darkMode = false,
                productIdentifier = productId
            ),
            type = type
        )
    }

    // endregion
}
