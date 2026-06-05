package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PaywallPreviewPresenterTest {

    private val paywallData = PaywallData(
        id = "abcd",
        templateName = "template1",
        config = TestData.template1Offering.paywall!!.config,
        localization = TestData.template1Offering.paywall!!.localizedConfiguration,
        assetBaseURL = URL("https://assets.pawwalls.com"),
    )

    private val offering = Offering(
        identifier = "1234",
        serverDescription = "Main offering",
        metadata = emptyMap(),
        availablePackages = TestData.template1Offering.availablePackages,
        paywall = paywallData,
    )

    private val mockActivity = mockk<Activity>(relaxed = true)

    @Before
    fun setUp() {
        // Makes MainScope().launch run the coroutine body synchronously, so tests
        // can assert on its effects without runTest or explicit advancement.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun intentFor(url: String): Intent =
        Intent().apply { data = Uri.parse(url) }

    private fun presenterFor(
        launched: (offeringId: String) -> Unit = {},
    ) = PaywallPreviewPresenter(
        launchPaywall = { _, offeringId -> launched(offeringId) },
    )

    // region Synchronous guard checks

    @Test
    fun `no intent data returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = Intent(),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `unrecognised host returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://NOT_THE_RIGHT_HOST?offering_id=1234&paywall_id=abcd"),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `too few query parameters returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234"),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `too many query parameters returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd&extra=0000"),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `empty offering_id returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=&paywall_id=abcd"),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `empty paywall_id returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id="),
            activity = mockActivity,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `null activity returns false`() {
        val result = presenterFor().handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd"),
            activity = null,
        )
        assertThat(result).isFalse()
    }

    // endregion

    // region Async behaviour

    @Test
    fun `valid url returns true and calls locateOffering with correct id`() {
        var locatedOfferingId: String? = null

        val result = presenterFor().handle(
            locateOffering = { offeringId ->
                locatedOfferingId = offeringId
                null
            },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd"),
            activity = mockActivity,
        )

        assertThat(result).isTrue()
        assertThat(locatedOfferingId).isEqualTo("1234")
    }

    @Test
    fun `failing to locate offering does not launch paywall`() {
        var launched = false

        presenterFor(launched = { launched = true }).handle(
            locateOffering = { throw RuntimeException("network error") },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd"),
            activity = mockActivity,
        )

        assertThat(launched).isFalse()
    }

    @Test
    fun `null offering does not launch paywall`() {
        var launched = false

        presenterFor(launched = { launched = true }).handle(
            locateOffering = { null },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd"),
            activity = mockActivity,
        )

        assertThat(launched).isFalse()
    }

    @Test
    fun `wrong paywall id does not launch paywall`() {
        var launched = false

        presenterFor(launched = { launched = true }).handle(
            locateOffering = { offering }, // offering has paywall id "abcd"
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=wxyz"),
            activity = mockActivity,
        )

        assertThat(launched).isFalse()
    }

    @Test
    fun `happy path launches paywall with correct offering id`() {
        var launchedOfferingId: String? = null

        val result = presenterFor(launched = { launchedOfferingId = it }).handle(
            locateOffering = { offering },
            intent = intentFor("rc://rc-paywall-preview?offering_id=1234&paywall_id=abcd"),
            activity = mockActivity,
        )

        assertThat(result).isTrue()
        assertThat(launchedOfferingId).isEqualTo("1234")
    }

    // endregion
}
