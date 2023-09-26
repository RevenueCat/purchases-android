package com.revenuecat.purchases

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.PlatformInfo
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
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
        Assertions.assertThat(purchases.purchasesOrchestrator.state.appInBackground).isFalse
    }

    @Test
    fun `state appInBackground is updated when app backgrounded`() {
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppBackgrounded()
        Assertions.assertThat(purchases.purchasesOrchestrator.state.appInBackground).isTrue
    }

    @Test
    fun `force update of caches when app foregrounded for the first time`() {
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = true)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        Assertions.assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
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
        Assertions.assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
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
        Assertions.assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse
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
        resetShowDeclinedPaymentMessagesAutomatically(false)
        purchases.purchasesOrchestrator.onActivityStarted(mockk())
        verify(exactly = 0) { mockBillingAbstract.showInAppMessagesIfNeeded(any()) }
    }

    @Test
    fun `activity on start shows inapp messages if option enabled`() {
        resetShowDeclinedPaymentMessagesAutomatically(true)
        val activity = mockk<Activity>()
        every { mockBillingAbstract.showInAppMessagesIfNeeded(activity) } just Runs
        purchases.purchasesOrchestrator.onActivityStarted(activity)
        verify(exactly = 1) { mockBillingAbstract.showInAppMessagesIfNeeded(activity) }
    }

    // endregion activity lifecycle

    // region Private

    private fun resetShowDeclinedPaymentMessagesAutomatically(showDeclinedPaymentMessagesAutomatically: Boolean) {
        purchases.purchasesOrchestrator.appConfig = AppConfig(
            mockContext,
            observerMode = false,
            showDeclinedPaymentMessagesAutomatically = showDeclinedPaymentMessagesAutomatically,
            PlatformInfo("", null),
            proxyURL = null,
            Store.AMAZON
        )
    }

    // endregion Private
}
