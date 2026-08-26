package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.copy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class WebCheckoutUrlResolverTest {

    @Test
    fun `custom checkout adds and encodes all configured parameters`() {
        val pkg = TestData.Packages.annual.copy(identifier = "Annual Trial & Intro")
        val state = FakePaywallState(purchases = MockPurchasesType(appUserID = "user /+?&"))
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout",
            rcPackage = pkg,
            packageParam = "package name",
            appUserIdParam = "user id",
            envParam = "environment",
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo(
            "https://example.com/checkout" +
                "?rc_source=app&user%20id=user%20%2F%2B%3F%26&environment=production" +
                "&package%20name=Annual%20Trial%20%26%20Intro",
        )
    }

    @Test
    fun `custom checkout reads the app user ID each time the URL is resolved`() {
        val purchases = MockPurchasesType(appUserID = "before-login")
        val state = FakePaywallState(purchases = purchases)
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout",
            appUserIdParam = "user",
        )

        assertThat(state.resolveWebCheckoutUrlForInteraction(action))
            .isEqualTo("https://example.com/checkout?rc_source=app&user=before-login")

        purchases.appUserID = "after-login"

        assertThat(state.resolveWebCheckoutUrlForInteraction(action))
            .isEqualTo("https://example.com/checkout?rc_source=app&user=after-login")
    }

    @Test
    fun `custom checkout with null configured names only adds source`() {
        val state = FakePaywallState()
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout",
            rcPackage = TestData.Packages.monthly,
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo("https://example.com/checkout?rc_source=app")
    }

    @Test
    fun `custom checkout without a package omits the configured package parameter`() {
        val state = FakePaywallState()
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout",
            packageParam = "package",
            envParam = "environment",
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo(
            "https://example.com/checkout?rc_source=app&environment=production",
        )
    }

    @Test
    fun `custom checkout preserves unrelated query parameters and fragment`() {
        val state = FakePaywallState()
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout?campaign=summer#details",
            rcPackage = TestData.Packages.monthly,
            packageParam = "package",
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo(
            "https://example.com/checkout?campaign=summer&rc_source=app&package=%24rc_monthly#details",
        )
    }

    @Test
    fun `custom checkout replaces all existing parameters with configured names`() {
        val state = FakePaywallState()
        val action = customCheckoutAction(
            customUrl = "https://example.com/checkout" +
                "?keep=one&rc_source=dashboard&package%20name=old" +
                "&keep_two=two&package%20name=older#details",
            rcPackage = TestData.Packages.monthly,
            packageParam = "package name",
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo(
            "https://example.com/checkout" +
                "?keep=one&rc_source=app&package%20name=%24rc_monthly&keep_two=two#details",
        )
    }

    @Test
    fun `invalid custom checkout URL falls back to package web checkout URL unchanged`() {
        val state = FakePaywallState()
        val action = customCheckoutAction(
            customUrl = "https://exa mple.com/checkout",
            rcPackage = TestData.Packages.monthly,
            packageParam = "package",
            envParam = "environment",
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo("https://test-web-billing.revenuecat.com?rc_package=\$rc_monthly")
    }

    @Test
    fun `RevenueCat Billing package checkout URL remains unchanged`() {
        val state = FakePaywallState()
        val action = PaywallAction.External.LaunchWebCheckout(
            customUrl = null,
            openMethod = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
            autoDismiss = false,
            packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append(
                rcPackage = TestData.Packages.monthly,
                packageParam = "package",
                envParam = "environment",
            ),
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo("https://test-web-billing.revenuecat.com?rc_package=\$rc_monthly")
    }

    @Test
    fun `RevenueCat Billing product selection URL remains unchanged`() {
        val state = FakePaywallState(
            offeringWebCheckoutURL = URL("https://pay.revenuecat.com/offering?existing=value#details"),
        )
        val action = PaywallAction.External.LaunchWebCheckout(
            customUrl = null,
            openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
            autoDismiss = false,
            packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend,
        )

        val result = state.resolveWebCheckoutUrlForInteraction(action)

        assertThat(result).isEqualTo("https://pay.revenuecat.com/offering?existing=value#details")
    }

    private fun customCheckoutAction(
        customUrl: String,
        rcPackage: Package? = null,
        packageParam: String? = null,
        appUserIdParam: String? = null,
        envParam: String? = null,
    ) = PaywallAction.External.LaunchWebCheckout(
        customUrl = customUrl,
        openMethod = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
        autoDismiss = false,
        packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append(
            rcPackage = rcPackage,
            packageParam = packageParam,
            appUserIdParam = appUserIdParam,
            envParam = envParam,
        ),
    )
}
