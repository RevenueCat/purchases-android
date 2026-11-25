package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
class PromotionalEntitlementExpirationSchedulerTest {

    private val startDate = Date(1_700_000_000_000)
    private var now = startDate
    private val dateProvider = object : DateProvider {
        override val now: Date get() = this@PromotionalEntitlementExpirationSchedulerTest.now
    }

    private var expirationsReached = 0

    private lateinit var scheduler: PromotionalEntitlementExpirationScheduler

    @Before
    fun setUp() {
        scheduler = PromotionalEntitlementExpirationScheduler(Handler(Looper.getMainLooper()), dateProvider)
    }

    @Test
    fun `reports when the promotional entitlement expires`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))

        advanceTimeBy(1.hours - 1.milliseconds)
        assertThat(expirationsReached).isEqualTo(0)

        advanceTimeBy(1.milliseconds)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `reports the earliest promotional expiration only`() {
        track(
            customerInfo(
                entitlement(expiresIn = 2.hours),
                entitlement(expiresIn = 1.hours),
            ),
        )

        advanceTimeBy(1.hours)
        assertThat(expirationsReached).isEqualTo(1)

        advanceTimeBy(1.days)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `reports right away if the promotional entitlement already expired`() {
        track(customerInfo(entitlement(expiresIn = (-1).minutes)))

        advanceTimeBy(Duration.ZERO)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `does not report entitlements from other stores`() {
        track(customerInfo(entitlement(expiresIn = 1.hours, store = Store.PLAY_STORE)))

        advanceTimeBy(1.days)
        assertThat(expirationsReached).isEqualTo(0)
    }

    @Test
    fun `does not report promotional entitlements that never expire`() {
        track(customerInfo(entitlement(expiresIn = null)))

        advanceTimeBy(1.days)
        assertThat(expirationsReached).isEqualTo(0)
    }

    @Test
    fun `does not report inactive promotional entitlements`() {
        track(customerInfo(entitlement(expiresIn = 1.hours, isActive = false)))

        advanceTimeBy(1.days)
        assertThat(expirationsReached).isEqualTo(0)
    }

    @Test
    fun `tracking a new customer info replaces the previous one`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))
        track(customerInfo(entitlement(expiresIn = 3.hours)))

        advanceTimeBy(2.hours)
        assertThat(expirationsReached).isEqualTo(0)

        advanceTimeBy(1.hours)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `tracking a customer info without promotional entitlements stops reporting`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))
        track(customerInfo())

        advanceTimeBy(1.days)
        assertThat(expirationsReached).isEqualTo(0)
    }

    @Test
    fun `rescheduling reports an expiration that passed while the handler was not advancing`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))

        // The wall clock moves on in deep sleep, the handler's uptime does not.
        now = Date(now.time + 2.hours.inWholeMilliseconds)
        advanceTimeBy(Duration.ZERO)
        assertThat(expirationsReached).isEqualTo(0)

        scheduler.reschedule()
        advanceTimeBy(Duration.ZERO)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `rescheduling schedules again from the wall clock`() {
        track(customerInfo(entitlement(expiresIn = 3.hours)))

        now = Date(now.time + 2.hours.inWholeMilliseconds)
        scheduler.reschedule()

        advanceTimeBy(1.hours - 1.milliseconds)
        assertThat(expirationsReached).isEqualTo(0)

        advanceTimeBy(1.milliseconds)
        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `rescheduling after an expiration was reported does not report it again`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))
        advanceTimeBy(1.hours)

        scheduler.reschedule()
        advanceTimeBy(1.days)

        assertThat(expirationsReached).isEqualTo(1)
    }

    @Test
    fun `closing stops reporting`() {
        track(customerInfo(entitlement(expiresIn = 1.hours)))
        scheduler.close()

        advanceTimeBy(1.days)
        scheduler.reschedule()
        advanceTimeBy(1.days)

        assertThat(expirationsReached).isEqualTo(0)
    }

    @Test
    fun `tracking after closing does not report`() {
        scheduler.close()
        track(customerInfo(entitlement(expiresIn = 1.hours)))

        advanceTimeBy(1.days)

        assertThat(expirationsReached).isEqualTo(0)
    }

    private fun track(customerInfo: CustomerInfo) {
        scheduler.track(customerInfo) { expirationsReached++ }
    }

    private fun advanceTimeBy(duration: Duration) {
        now = Date(now.time + duration.inWholeMilliseconds)
        shadowOf(Looper.getMainLooper()).idleFor(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    private fun entitlement(
        expiresIn: Duration?,
        store: Store = Store.PROMOTIONAL,
        isActive: Boolean = true,
    ): EntitlementInfo = mockk<EntitlementInfo>().also {
        every { it.store } returns store
        every { it.isActive } returns isActive
        every { it.expirationDate } returns expiresIn?.let { Date(startDate.time + it.inWholeMilliseconds) }
    }

    private fun customerInfo(vararg entitlements: EntitlementInfo): CustomerInfo = mockk<CustomerInfo>().also {
        every { it.entitlements } returns EntitlementInfos(
            entitlements.withIndex().associate { (index, entitlement) -> "entitlement_$index" to entitlement },
            VerificationResult.NOT_REQUESTED,
        )
    }
}
