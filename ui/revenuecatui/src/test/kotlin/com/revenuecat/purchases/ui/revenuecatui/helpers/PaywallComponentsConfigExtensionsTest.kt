@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class PaywallComponentsConfigExtensionsTest {

    // region positive cases — main stack

    @Test
    fun `PackageComponent in main stack is a paywall`() {
        assertThat(configWith(pkg()).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `PurchaseButtonComponent in main stack is a paywall`() {
        val purchaseButton = PurchaseButtonComponent(stack = emptyStack())
        assertThat(configWith(purchaseButton).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `PackageComponent nested inside a StackComponent is a paywall`() {
        val outer = StackComponent(components = listOf(StackComponent(components = listOf(pkg()))))
        assertThat(configWith(outer).containsPaywallEventComponent()).isTrue()
    }

    // endregion

    // region ButtonComponent

    @Test
    fun `ButtonComponent whose body stack contains a package is a paywall`() {
        val button = ButtonComponent(
            action = ButtonComponent.Action.RestorePurchases,
            stack = StackComponent(components = listOf(pkg())),
        )
        assertThat(configWith(button).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `ButtonComponent with Sheet destination containing a package is a paywall`() {
        val sheet = ButtonComponent.Destination.Sheet(
            id = "sheet-1",
            name = null,
            stack = StackComponent(components = listOf(pkg())),
            backgroundBlur = false,
            size = null,
        )
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(destination = sheet),
            stack = emptyStack(),
        )
        assertThat(configWith(button).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `ButtonComponent navigating to a URL is not a paywall`() {
        val url = ButtonComponent.Destination.Url(
            urlLid = LocalizationKey("url_key"),
            method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
        )
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(destination = url),
            stack = emptyStack(),
        )
        assertThat(configWith(button).containsPaywallEventComponent()).isFalse()
    }

    @Test
    fun `ButtonComponent navigating to CustomerCenter is not a paywall`() {
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                destination = ButtonComponent.Destination.CustomerCenter,
            ),
            stack = emptyStack(),
        )
        assertThat(configWith(button).containsPaywallEventComponent()).isFalse()
    }

    // endregion

    // region CarouselComponent

    @Test
    fun `CarouselComponent with a page containing a package is a paywall`() {
        val carousel = CarouselComponent(
            pages = listOf(
                emptyStack(),
                StackComponent(components = listOf(pkg())),
            ),
            pageAlignment = VerticalAlignment.TOP,
        )
        assertThat(configWith(carousel).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `CarouselComponent with no package pages is not a paywall`() {
        val carousel = CarouselComponent(
            pages = listOf(emptyStack(), emptyStack()),
            pageAlignment = VerticalAlignment.TOP,
        )
        assertThat(configWith(carousel).containsPaywallEventComponent()).isFalse()
    }

    // endregion

    // region TabsComponent

    @Test
    fun `TabsComponent with a tab containing a package is a paywall`() {
        val tabs = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(id = "tab-1", name = null, stack = emptyStack()),
                TabsComponent.Tab(id = "tab-2", name = null, stack = StackComponent(components = listOf(pkg()))),
            ),
            control = TabsComponent.TabControl.Buttons(stack = emptyStack()),
        )
        assertThat(configWith(tabs).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `TabsComponent Buttons control containing a package is a paywall`() {
        val tabs = TabsComponent(
            tabs = listOf(TabsComponent.Tab(id = "tab-1", name = null, stack = emptyStack())),
            control = TabsComponent.TabControl.Buttons(
                stack = StackComponent(components = listOf(pkg())),
            ),
        )
        assertThat(configWith(tabs).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `TabsComponent Toggle control containing a package is a paywall`() {
        val tabs = TabsComponent(
            tabs = listOf(TabsComponent.Tab(id = "tab-1", name = null, stack = emptyStack())),
            control = TabsComponent.TabControl.Toggle(
                stack = StackComponent(components = listOf(pkg())),
            ),
        )
        assertThat(configWith(tabs).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `TabsComponent with no packages anywhere is not a paywall`() {
        val tabs = TabsComponent(
            tabs = listOf(TabsComponent.Tab(id = "tab-1", name = null, stack = emptyStack())),
            control = TabsComponent.TabControl.Buttons(stack = emptyStack()),
        )
        assertThat(configWith(tabs).containsPaywallEventComponent()).isFalse()
    }

    // endregion

    // region CountdownComponent

    @Test
    fun `CountdownComponent with a package in countdownStack is a paywall`() {
        val countdown = CountdownComponent(
            style = countdownStyle(),
            countdownStack = StackComponent(components = listOf(pkg())),
        )
        assertThat(configWith(countdown).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `CountdownComponent with a package in endStack is a paywall`() {
        val countdown = CountdownComponent(
            style = countdownStyle(),
            countdownStack = emptyStack(),
            endStack = StackComponent(components = listOf(pkg())),
        )
        assertThat(configWith(countdown).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `CountdownComponent with a package in fallback stack is a paywall`() {
        val countdown = CountdownComponent(
            style = countdownStyle(),
            countdownStack = emptyStack(),
            fallback = StackComponent(components = listOf(pkg())),
        )
        assertThat(configWith(countdown).containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `CountdownComponent with no packages is not a paywall`() {
        val countdown = CountdownComponent(
            style = countdownStyle(),
            countdownStack = emptyStack(),
        )
        assertThat(configWith(countdown).containsPaywallEventComponent()).isFalse()
    }

    // endregion

    // region header and stickyFooter (top-level config slots)

    @Test
    fun `package in header stack is a paywall`() {
        val config = PaywallComponentsConfig(
            stack = emptyStack(),
            background = background(),
            stickyFooter = null,
            header = HeaderComponent(stack = StackComponent(components = listOf(pkg()))),
        )
        assertThat(config.containsPaywallEventComponent()).isTrue()
    }

    @Test
    fun `package in stickyFooter stack is a paywall`() {
        val config = PaywallComponentsConfig(
            stack = emptyStack(),
            background = background(),
            stickyFooter = StickyFooterComponent(stack = StackComponent(components = listOf(pkg()))),
        )
        assertThat(config.containsPaywallEventComponent()).isTrue()
    }

    // endregion

    // region negative — no paywall components at all

    @Test
    fun `empty config is not a paywall`() {
        val config = PaywallComponentsConfig(
            stack = emptyStack(),
            background = background(),
            stickyFooter = null,
        )
        assertThat(config.containsPaywallEventComponent()).isFalse()
    }

    // endregion

    // region helpers

    private fun pkg() = PackageComponent(
        packageId = PackageType.MONTHLY.identifier!!,
        isSelectedByDefault = false,
        stack = emptyStack(),
    )

    private fun emptyStack() = StackComponent(components = emptyList())

    private fun background() = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt())))

    private fun configWith(vararg components: com.revenuecat.purchases.paywalls.components.PaywallComponent) =
        PaywallComponentsConfig(
            stack = StackComponent(components = components.toList()),
            background = background(),
            stickyFooter = null,
        )

    private fun countdownStyle() = CountdownComponent.CountdownStyle(
        type = "fixed_date",
        date = Date(0),
    )

    // endregion
}
