package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.models.Transaction
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerInfoSeeAllPurchasesTest {

    private fun customerInfoWith(
        subscriptionProductIds: List<String>,
        activeSubscriptionProductIds: Set<String>,
        nonSubscriptionCount: Int = 0,
    ): CustomerInfo = mockk(relaxed = true) {
        every { subscriptionsByProductIdentifier } returns subscriptionProductIds
            .associateWith { mockk<SubscriptionInfo>(relaxed = true) }
        every { activeSubscriptions } returns activeSubscriptionProductIds
        every { nonSubscriptionTransactions } returns List(nonSubscriptionCount) {
            mockk<Transaction>(relaxed = true)
        }
    }

    @Test
    fun `returns false with no purchases`() {
        val customerInfo = customerInfoWith(emptyList(), emptySet())
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isFalse()
    }

    @Test
    fun `returns false with only one active subscription`() {
        val customerInfo = customerInfoWith(listOf("active"), setOf("active"))
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isFalse()
    }

    @Test
    fun `returns true with one active and one inactive subscription`() {
        val customerInfo = customerInfoWith(listOf("active", "inactive"), setOf("active"))
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }

    @Test
    fun `returns false with only one inactive subscription and no active`() {
        val customerInfo = customerInfoWith(listOf("inactive"), emptySet())
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isFalse()
    }

    @Test
    fun `returns true with two or more inactive subscriptions and no active`() {
        val customerInfo = customerInfoWith(listOf("inactive_a", "inactive_b"), emptySet())
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }

    @Test
    fun `returns false with exactly two non-subscriptions and no subs`() {
        val customerInfo = customerInfoWith(emptyList(), emptySet(), nonSubscriptionCount = 2)
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isFalse()
    }

    @Test
    fun `returns true with more than two non-subscriptions`() {
        val customerInfo = customerInfoWith(emptyList(), emptySet(), nonSubscriptionCount = 3)
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }

    @Test
    fun `non-subscriptions count independently from subscription conditions`() {
        val customerInfo = customerInfoWith(listOf("active"), setOf("active"), nonSubscriptionCount = 3)
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }

    @Test
    fun `counts a promotional subscription as a subscription`() {
        // The promotional grant is active and the paid one is expired, so there is more to show
        // than the relevant purchases screen lists. PurchaseInformation.isSubscription would
        // drop the promotional purchase into the non-subscription bucket and hide the link.
        val customerInfo = customerInfoWith(listOf("promo", "expired_paid"), setOf("promo"))
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }

    @Test
    fun `counts subscriptions as inactive even when they still back an active entitlement`() {
        // PurchaseInformation.isExpired follows entitlement activity, which would count both of
        // these as active and hide the link.
        val customerInfo = customerInfoWith(listOf("expired_a", "expired_b"), emptySet())
        assertThat(customerInfo.shouldShowSeeAllPurchases()).isTrue()
    }
}
