package com.revenuecat.purchases.ui.revenuecatui.data

import android.os.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseContext
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.createGoogleStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class PurchaseContextTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual

    private fun pkg(value: Package, selected: Boolean = false) = PackageComponent(
        packageId = value.identifier,
        isSelectedByDefault = selected,
        stack = StackComponent(emptyList()),
    )

    private fun scope(vararg components: PaywallComponent) = StackComponent(
        components = components.toList(),
        purchaseContext = PurchaseContext(mode = "independent"),
    )

    private fun tabbedSheetContext(): Pair<PaywallState.Loaded.Components, PaywallState.Loaded.Components> {
        val sheetContent = scope(
            pkg(monthly, selected = true),
            TabsComponent(
                control = TabsComponent.TabControl.Buttons(StackComponent(emptyList())),
                tabs = listOf(
                    TabsComponent.Tab(id = "monthly", stack = StackComponent(listOf(pkg(monthly, selected = true)))),
                    TabsComponent.Tab(id = "weekly", stack = StackComponent(listOf(
                        pkg(TestData.Packages.weekly, selected = true), pkg(annual),
                    ))),
                ),
            ),
        )
        val parent = FakePaywallState(
            components = listOf(pkg(annual, selected = true), ButtonComponent(
                stack = StackComponent(emptyList()),
                action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                    id = "tabbed-sheet", stack = sheetContent,
                )),
            )),
            packages = listOf(monthly, annual, TestData.Packages.weekly),
            addPackageComponents = false,
        )
        val button = (parent.stack as StackComponentStyle).children[1] as ButtonComponentStyle
        val destination = (button.action as ButtonComponentStyle.Action.NavigateTo)
            .destination as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        parent.sheet.show(backgroundBlur = false, content = {}, presentationKey = destination.presentationKey)
        return parent to requireNotNull(parent.purchaseContextState(destination.stack as StackComponentStyle))
    }

    @Test
    fun `a tab default in an independent sheet takes precedence over its outside default`() {
        val (parent, child) = tabbedSheetContext()
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        child.update(selectedTabIndex = 1)
        child.reconcilePurchaseContextSelection()
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.weekly)
        assertThat(parent.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `an independent sheet remembers a manually selected package in its own tabs`() {
        val (parent, child) = tabbedSheetContext()
        child.update(selectedTabIndex = 1)
        child.update(selectedPackageUniqueId = annual.identifier)
        child.update(selectedTabIndex = 0)
        child.update(selectedTabIndex = 1)
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        assertThat(parent.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `child package visibility cannot make a hidden parent default eligible`() {
        val monthly = Package(
            monthly.identifier,
            monthly.packageType,
            createGoogleStoreProduct(productId = monthly.product.id, basePlanId = "monthly"),
            monthly.presentedOfferingContext,
            monthly.webCheckoutURL,
        )
        val parent = FakePaywallState(
            components = listOf(
                StackComponent(listOf(pkg(monthly, selected = true)), visible = false),
                pkg(annual),
                scope(pkg(monthly, selected = true)),
            ),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
        )
        parent.reconcileSelectionForWindowSize(DpSize(400.dp, 800.dp))
        val childStyle = (parent.stack as StackComponentStyle).children[2] as StackComponentStyle
        val child = requireNotNull(parent.purchaseContextState(childStyle))
        assertThat(parent.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `a default in a closed nested sheet must not become the outer context selection`() {
        val nested = ButtonComponent(
            stack = StackComponent(emptyList()),
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "nested-sheet",
                stack = StackComponent(listOf(pkg(annual, selected = true))),
            )),
        )
        val parent = FakePaywallState(
            components = listOf(scope(pkg(monthly), nested)),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
        )
        val style = (parent.stack as StackComponentStyle).children[0] as StackComponentStyle
        val child = requireNotNull(parent.purchaseContextState(style))
        assertThat(child.sheet.visible).isFalse()
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

        val button = style.children[1] as ButtonComponentStyle
        val destination = (button.action as ButtonComponentStyle.Action.NavigateTo)
            .destination as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        child.sheet.show(backgroundBlur = false, content = {}, presentationKey = destination.presentationKey)
        child.reconcilePurchaseContextSelection(initialize = true)
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        child.sheet.hide()
        child.reconcilePurchaseContextSelection()
        assertThat(child.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `a scope inside a sheet must select its visible package after paths are attached`() {
        val nested = ButtonComponent(
            stack = StackComponent(emptyList()),
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "legacy-sheet",
                stack = StackComponent(listOf(scope(pkg(monthly, selected = true)))),
            )),
        )
        val parent = FakePaywallState(
            components = listOf(pkg(annual, selected = true), nested),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
        )
        val button = (parent.stack as StackComponentStyle).children[1]
            as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo)
            .destination as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        val style = (sheet.stack as StackComponentStyle).children[0] as StackComponentStyle
        assertThat(parent.purchaseContextState(style)?.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `an existing independent context follows the paywalls locale updates`() {
        val parent = FakePaywallState(
            components = listOf(scope(pkg(monthly, selected = true))),
            packages = listOf(monthly),
            localizations = nonEmptyMapOf(
                LocaleId("en_US") to nonEmptyMapOf(LocalizationKey("label") to LocalizationData.Text("English")),
                LocaleId("fr_FR") to nonEmptyMapOf(LocalizationKey("label") to LocalizationData.Text("French")),
            ),
            addPackageComponents = false,
        )
        val style = (parent.stack as StackComponentStyle).children[0] as StackComponentStyle
        val child = requireNotNull(parent.purchaseContextState(style))
        val grandchild = requireNotNull(child.purchaseContextState(style))
        parent.update(localeList = LocaleList(Locale.forLanguageTag("fr-FR")))
        assertThat(child.locale).isEqualTo(parent.locale)
        assertThat(grandchild.locale).isEqualTo(parent.locale)
    }
}
