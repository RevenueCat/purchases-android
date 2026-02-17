package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptySetOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Tests for [PaywallState.Loaded.Components] package selection behavior.
 *
 * These tests verify the fix for MON-1823: paywall view not showing prices in variables correctly
 * on initial load when packages are only in tabs and no initialSelectedTabIndex is provided.
 */
@RunWith(RobolectricTestRunner::class)
internal class PaywallStateLoadedComponentsPackageSelectionTests {

    private val localeId = LocaleId("en_US")

    @Test
    fun `Should select default package from tab 0 when initialSelectedTabIndex is null`() {
        // Arrange: packages only in tabs, no initialSelectedTabIndex
        val defaultPackage = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(defaultPackage, isSelectedByDefault = true),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null, // This is the key - null initialSelectedTabIndex
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackage)
    }

    @Test
    fun `Should select first package from tab 0 when no package is marked as default`() {
        // Arrange: no package marked as default
        val firstPackage = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(firstPackage, isSelectedByDefault = false),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(firstPackage)
    }

    @Test
    fun `Should select package outside tabs when available and marked as default`() {
        // Arrange: package outside tabs is marked as default
        val defaultPackageOutsideTabs = TestData.Packages.weekly
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(defaultPackageOutsideTabs, isSelectedByDefault = true),
            ),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOutsideTabs)
    }

    @Test
    fun `Should select package from specified tab when initialSelectedTabIndex is provided`() {
        // Arrange: initialSelectedTabIndex points to tab 1
        val defaultPackageOnTab1 = TestData.Packages.annual
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = true),
                ),
                1 to listOf(
                    packageInfo(defaultPackageOnTab1, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = 1,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOnTab1)
    }

    @Test
    fun `Should have null selectedPackageInfo when no packages are available`() {
        // Arrange: no packages at all
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `Should select package from tab 0 even when tab 1 has default package and initialSelectedTabIndex is null`() {
        // Arrange: tab 1 has a default package, but initialSelectedTabIndex is null (defaults to tab 0)
        val defaultPackageOnTab0 = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(defaultPackageOnTab0, isSelectedByDefault = true),
                ),
                1 to listOf(
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOnTab0)
        assertThat(state.selectedTabIndex).isEqualTo(0)
    }

    private fun packageInfo(
        pkg: com.revenuecat.purchases.Package,
        isSelectedByDefault: Boolean,
    ) = PaywallState.Loaded.Components.AvailablePackages.Info(
        pkg = pkg,
        isSelectedByDefault = isSelectedByDefault,
    )

    private fun paywallState(
        packagesOutsideTabs: List<PaywallState.Loaded.Components.AvailablePackages.Info>,
        packagesByTab: Map<Int, List<PaywallState.Loaded.Components.AvailablePackages.Info>>,
        initialSelectedTabIndex: Int?,
    ) = PaywallState.Loaded.Components(
        stack = previewStackComponentStyle(children = emptyList()),
        stickyFooter = null,
        background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.White))),
        showPricesWithDecimals = true,
        variableConfig = UiConfig.VariableConfig(),
        variableDataProvider = VariableDataProvider(MockResourceProvider()),
        offering = Offering(
            identifier = "test-offering",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = (packagesOutsideTabs.map { it.pkg } +
                packagesByTab.values.flatten().map { it.pkg }).distinct(),
            paywall = null,
            paywallComponents = null,
        ),
        locales = nonEmptySetOf(localeId),
        storefrontCountryCode = "US",
        dateProvider = { Date() },
        packages = PaywallState.Loaded.Components.AvailablePackages(
            packagesOutsideTabs = packagesOutsideTabs,
            packagesByTab = packagesByTab,
        ),
        initialLocaleList = LocaleList("en-US"),
        initialSelectedTabIndex = initialSelectedTabIndex,
        purchases = MockPurchasesType(),
    )
}
