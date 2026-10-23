package com.revenuecat.purchases.ui.revenuecatui.views

import android.os.Looper
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ExitOffer
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
class PaywallViewExitOfferTest {

    private val mockPurchases = mockk<Purchases>()

    private val exitOffering = TestData.template2Offering
    private val defaultLocaleIdentifier = LocaleId("en_US")
    private val offeringWithExitOffer = Offering(
        identifier = "offering-with-exit-offer",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(
            UiConfig(),
            PaywallComponentsData(
                id = "paywall_id",
                templateName = "template",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        // Need to have at least one PackageComponent for the PaywallState to be valid.
                        stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
                        background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                        stickyFooter = null,
                    ),
                ),
                componentsLocalizations = nonEmptyMapOf(
                    defaultLocaleIdentifier to nonEmptyMapOf(
                        LocalizationKey("dummy_text") to LocalizationData.Text("dummy text"),
                    ),
                ),
                defaultLocaleIdentifier = defaultLocaleIdentifier,
                exitOffers = ExitOffers(dismiss = ExitOffer(offeringId = exitOffering.identifier)),
            ),
        ),
    )

    @Before
    fun setUp() {
        val all = listOf(offeringWithExitOffer, exitOffering, TestData.template1Offering)
        val offerings = Offerings(current = offeringWithExitOffer, all = all.associateBy { it.identifier })
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns mockPurchases
        every { mockPurchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
        every { mockPurchases.storefrontCountryCode } returns "US"
        every { mockPurchases.preferredUILocaleOverride } returns null
        every { mockPurchases.track(any()) } just Runs
        every { mockPurchases.currentConfiguration } returns mockk {
            every { dangerousSettings } returns DangerousSettings()
        }
        every { mockPurchases.getOfferings(any()) } answers {
            firstArg<ReceiveOfferingsCallback>().onReceived(offerings)
        }
        coEvery { mockPurchases.resolveWorkflow(any()) } returns WorkflowResolution.NoWorkflow
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `exit offer handler receives the exit offering instead of the dismiss handler`() {
        val receivedExitOfferings = mutableListOf<Offering>()
        var dismissCount = 0
        val viewModel = attachPaywallView(
            offering = offeringWithExitOffer,
            onDismiss = { dismissCount++ },
            onExitOffer = { receivedExitOfferings += it },
        )

        viewModel.closePaywall()

        assertThat(receivedExitOfferings.map { it.identifier }).containsExactly(exitOffering.identifier)
        assertThat(dismissCount).isZero()
    }

    @Test
    fun `without an exit offer handler the dismiss handler is called and no exit offering is preloaded`() {
        var dismissCount = 0
        val viewModel = attachPaywallView(
            offering = offeringWithExitOffer,
            onDismiss = { dismissCount++ },
            onExitOffer = null,
        )

        assertThat(viewModel.preloadedExitOffering).isNull()
        viewModel.closePaywall()

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `exit offer handler is not called when the offering has no exit offer`() {
        val receivedExitOfferings = mutableListOf<Offering>()
        var dismissCount = 0
        val viewModel = attachPaywallView(
            offering = TestData.template1Offering,
            onDismiss = { dismissCount++ },
            onExitOffer = { receivedExitOfferings += it },
        )

        viewModel.closePaywall()

        assertThat(receivedExitOfferings).isEmpty()
        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `clearing the exit offer handler restores plain dismissal`() {
        val receivedExitOfferings = mutableListOf<Offering>()
        var dismissCount = 0
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val paywallView = PaywallView(activity)
        paywallView.setOfferingId(
            offeringWithExitOffer.identifier,
            PresentedOfferingContext(offeringWithExitOffer.identifier),
        )
        paywallView.setDismissHandler { dismissCount++ }
        paywallView.setExitOfferHandler { receivedExitOfferings += it }
        paywallView.setExitOfferHandler(null)
        container.addView(paywallView)
        shadowOf(Looper.getMainLooper()).idle()

        viewModelFor(activity).closePaywall()

        assertThat(receivedExitOfferings).isEmpty()
        assertThat(dismissCount).isEqualTo(1)
    }

    private fun attachPaywallView(
        offering: Offering,
        onDismiss: () -> Unit,
        onExitOffer: ((Offering) -> Unit)?,
    ): PaywallViewModelImpl {
        val activity = Robolectric.buildActivity(PaywallViewHostActivity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val paywallView = PaywallView(activity)
        paywallView.setOfferingId(offering.identifier, PresentedOfferingContext(offering.identifier))
        paywallView.setDismissHandler(onDismiss)
        paywallView.setExitOfferHandler(onExitOffer)
        container.addView(paywallView)
        shadowOf(Looper.getMainLooper()).idle()
        return viewModelFor(activity)
    }

    private fun viewModelFor(activity: PaywallViewHostActivity): PaywallViewModelImpl {
        val viewModelKey = activity.viewModelStore.keys().single { key -> key.toIntOrNull() != null }
        val viewModel = ViewModelProvider(activity).get(viewModelKey, PaywallViewModelImpl::class.java)
        assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)
        return viewModel
    }
}
