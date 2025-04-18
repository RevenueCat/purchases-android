package com.revenuecat.purchases

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.models.InAppMessageType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesLifecycleTest: BasePurchasesTest() {

    // region app lifecycle

    @Test
    fun `state appInBackground is updated when app foregrounded`() {
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = true)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.appInBackground).isFalse
    }

    @Test
    fun `state appInBackground is updated when app backgrounded`() {
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppBackgrounded()
        assertThat(purchases.purchasesOrchestrator.state.appInBackground).isTrue
    }

    @Test
    fun `appConfig isAppBackgrounded is updated when app foregrounded`() {
        mockOfferingsManagerAppForeground()
        assertThat(appConfig.isAppBackgrounded).isTrue
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(appConfig.isAppBackgrounded).isFalse
    }

    @Test
    fun `appConfig isAppBackgrounded is updated when app backgrounded`() {
        appConfig.isAppBackgrounded = false
        assertThat(appConfig.isAppBackgrounded).isFalse
        Purchases.sharedInstance.purchasesOrchestrator.onAppBackgrounded()
        assertThat(appConfig.isAppBackgrounded).isTrue
    }

    @Test
    fun `force update of caches when app foregrounded for the first time`() {
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = true)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 0) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `don't force update of caches when app foregrounded not for the first time`() {
        every {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns false
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
        verify(exactly = 0) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `update of caches when app foregrounded not for the first time and caches stale`() {
        every {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns true
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    // endregion

    // region activity lifecycle

    @Test
    fun `activity on start does not show inapp messages if option disabled`() {
        resetShowInAppMessagesAutomatically(false)
        purchases.purchasesOrchestrator.onActivityStarted(mockk())
        verify(exactly = 0) { mockBillingAbstract.showInAppMessagesIfNeeded(any(), any(), any()) }
    }

    @Test
    fun `activity on start shows inapp messages if option enabled`() {
        resetShowInAppMessagesAutomatically(true)
        val activity = mockk<Activity>()
        every { mockBillingAbstract.showInAppMessagesIfNeeded(activity, any(), any()) } just Runs
        purchases.purchasesOrchestrator.onActivityStarted(activity)
        verify(exactly = 1) { mockBillingAbstract.showInAppMessagesIfNeeded(activity, any(), any()) }
    }

    @Test
    fun `activity on start shows all inapp message types if option enabled`() {
        resetShowInAppMessagesAutomatically(true)
        val activity = mockk<Activity>()
        val types = InAppMessageType.values().toList()
        val typesSlot = slot<List<InAppMessageType>>()
        every { mockBillingAbstract.showInAppMessagesIfNeeded(activity, capture(typesSlot), any()) } just Runs
        purchases.purchasesOrchestrator.onActivityStarted(activity)
        assertThat(typesSlot.captured).containsExactlyInAnyOrderElementsOf(types)
    }

    @Test
    fun `activity on start syncs purchases if subscription status changed after showing inapp messages`() {
        resetShowInAppMessagesAutomatically(true)
        val activity = mockk<Activity>()
        val subscriptionStatusChangeSlot = slot<() -> Unit>()
        every {
            mockBillingAbstract.showInAppMessagesIfNeeded(activity, any(), capture(subscriptionStatusChangeSlot))
        } just Runs
        purchases.purchasesOrchestrator.onActivityStarted(activity)
        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs
        subscriptionStatusChangeSlot.captured.invoke()
        verify(exactly = 1) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }
    }

    // endregion activity lifecycle

    // region Private

    private fun resetShowInAppMessagesAutomatically(showInAppMessagesAutomatically: Boolean) {
        purchases.purchasesOrchestrator.appConfig = AppConfig(
            mockContext,
            PurchasesAreCompletedBy.REVENUECAT,
            showInAppMessagesAutomatically = showInAppMessagesAutomatically,
            PlatformInfo("", null),
            proxyURL = null,
            Store.AMAZON,
            isDebugBuild = false,
        )
    }

    // endregion Private
}
