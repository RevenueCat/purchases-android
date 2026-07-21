package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterNavigationState
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerCenterStateShouldShowPurchaseHistoryTest {

    private val activeSub = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing   // isSubscription=true, isExpired=false
    private val inactiveSub = CustomerCenterConfigTestData.purchaseInformationYearlyExpired    // isSubscription=true, isExpired=true
    private val nonSub = CustomerCenterConfigTestData.purchaseInformationLifetime              // isSubscription=false

    private fun stateWith(
        displayPurchaseHistoryLink: Boolean?,
        allPurchases: List<PurchaseInformation>,
    ): CustomerCenterState.Success {
        val configData = mockk<CustomerCenterConfigData>(relaxed = true)
        every { configData.support } returns CustomerCenterConfigData.Support(
            displayPurchaseHistoryLink = displayPurchaseHistoryLink,
        )
        return CustomerCenterState.Success(
            customerCenterConfigData = configData,
            allPurchases = allPurchases,
            navigationState = CustomerCenterNavigationState(
                showingActivePurchasesScreen = false,
                managementScreenTitle = null,
            ),
        )
    }

    @Test
    fun `returns false when displayPurchaseHistoryLink is null`() {
        val state = stateWith(
            displayPurchaseHistoryLink = null,
            allPurchases = listOf(activeSub, inactiveSub, nonSub, nonSub, nonSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns false when displayPurchaseHistoryLink is false`() {
        val state = stateWith(
            displayPurchaseHistoryLink = false,
            allPurchases = listOf(activeSub, inactiveSub, nonSub, nonSub, nonSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns false when flag is true but allPurchases is empty`() {
        val state = stateWith(displayPurchaseHistoryLink = true, allPurchases = emptyList())
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns false with only one active subscription`() {
        val state = stateWith(displayPurchaseHistoryLink = true, allPurchases = listOf(activeSub))
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns true with one active and one inactive subscription`() {
        val state = stateWith(
            displayPurchaseHistoryLink = true,
            allPurchases = listOf(activeSub, inactiveSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isTrue()
    }

    @Test
    fun `returns false with only one inactive subscription and no active`() {
        val state = stateWith(displayPurchaseHistoryLink = true, allPurchases = listOf(inactiveSub))
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns true with two or more inactive subscriptions and no active`() {
        val state = stateWith(
            displayPurchaseHistoryLink = true,
            allPurchases = listOf(inactiveSub, inactiveSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isTrue()
    }

    @Test
    fun `returns false with exactly two non-subscriptions and no subs`() {
        val state = stateWith(
            displayPurchaseHistoryLink = true,
            allPurchases = listOf(nonSub, nonSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isFalse()
    }

    @Test
    fun `returns true with more than two non-subscriptions`() {
        val state = stateWith(
            displayPurchaseHistoryLink = true,
            allPurchases = listOf(nonSub, nonSub, nonSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isTrue()
    }

    @Test
    fun `non-subscriptions count independently from subscription conditions`() {
        val state = stateWith(
            displayPurchaseHistoryLink = true,
            allPurchases = listOf(activeSub, nonSub, nonSub, nonSub),
        )
        assertThat(state.shouldShowPurchaseHistory).isTrue()
    }
}
