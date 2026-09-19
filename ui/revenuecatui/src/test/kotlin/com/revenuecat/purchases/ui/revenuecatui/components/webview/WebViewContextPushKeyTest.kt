package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.os.LocaleList as FrameworkLocaleList
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptySetOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

/** A push that never fires is invisible in the bridge's own tests: the frames it sends are correct. */
@RunWith(RobolectricTestRunner::class)
internal class WebViewContextPushKeyTest {

    @Test
    fun `an unchanged state produces an equal key`() {
        val state = paywallState()

        assertThat(webViewContextPushKey(state, darkMode = false))
            .isEqualTo(webViewContextPushKey(state, darkMode = false))
    }

    @Test
    fun `a rebuilt state produces a different key`() {
        val before = paywallState(customVariables = emptyMap())
        val after = paywallState(
            customVariables = mapOf("org" to CustomVariableValue.String("RevenueCat")),
        )

        assertThat(webViewContextPushKey(before, darkMode = false))
            .isNotEqualTo(webViewContextPushKey(after, darkMode = false))
    }

    @Test
    fun `a dark mode change produces a different key`() {
        val state = paywallState()

        assertThat(webViewContextPushKey(state, darkMode = false))
            .isNotEqualTo(webViewContextPushKey(state, darkMode = true))
    }

    @Test
    fun `switching to a localized locale produces a different key`() {
        val state = paywallState()
        val before = webViewContextPushKey(state, darkMode = false)

        mutateState { state.update(localeList = FrameworkLocaleList.forLanguageTags("es-ES")) }

        assertThat(webViewContextPushKey(state, darkMode = false)).isNotEqualTo(before)
    }

    @Test
    fun `a locale the paywall has no dictionary for produces an equal key`() {
        // The snapshot reports the locale actually rendered, which falls back to an available one.
        val state = paywallState()
        val before = webViewContextPushKey(state, darkMode = false)

        mutateState { state.update(localeList = FrameworkLocaleList.forLanguageTags("de-DE")) }

        assertThat(webViewContextPushKey(state, darkMode = false)).isEqualTo(before)
    }

    @Test
    fun `selecting another package produces a different key`() {
        val state = paywallState()
        val before = webViewContextPushKey(state, darkMode = false)

        mutateState { state.update(selectedPackageUniqueId = TestData.Packages.annual.identifier) }

        assertThat(webViewContextPushKey(state, darkMode = false)).isNotEqualTo(before)
    }

    /**
     * A write outside a composition lands in the global snapshot; without an apply it stays pending
     * and every later Compose test in this JVM fails to reach idle.
     */
    private fun mutateState(block: () -> Unit) = Snapshot.withMutableSnapshot(block)

    private fun paywallState(
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
    ): PaywallState.Loaded.Components {
        val packages = listOf(TestData.Packages.monthly, TestData.Packages.annual).map { pkg ->
            PaywallState.Loaded.Components.AvailablePackages.Info(
                pkg = pkg,
                isSelectedByDefault = pkg == TestData.Packages.monthly,
            )
        }
        return PaywallState.Loaded.Components(
            stack = previewStackComponentStyle(children = emptyList()),
            header = null,
            stickyFooter = null,
            background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.White))),
            showPricesWithDecimals = true,
            variableConfig = UiConfig.VariableConfig(
                variableCompatibilityMap = emptyMap(),
                functionCompatibilityMap = emptyMap(),
            ),
            variableDataProvider = VariableDataProvider(MockResourceProvider()),
            offering = Offering(
                identifier = "test-offering",
                serverDescription = "description",
                metadata = emptyMap(),
                availablePackages = packages.map { it.pkg },
                paywall = null,
                paywallComponents = null,
            ),
            locales = nonEmptySetOf(LocaleId("en_US"), LocaleId("es_ES")),
            storefrontCountryCode = "US",
            dateProvider = { Date() },
            packages = PaywallState.Loaded.Components.AvailablePackages(
                packagesOutsideTabs = packages,
                packagesByTab = emptyMap(),
            ),
            initialLocaleList = LocaleList("en-US"),
            initialSelectedTabIndex = null,
            purchases = MockPurchasesType(),
            customVariables = customVariables,
        )
    }
}
