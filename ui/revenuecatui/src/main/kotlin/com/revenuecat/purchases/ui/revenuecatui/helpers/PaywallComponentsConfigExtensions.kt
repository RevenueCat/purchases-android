@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.FallbackHeaderComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig

@JvmSynthetic
internal fun PaywallComponentsConfig.containsPaywallEventComponent(): Boolean =
    stack.containsPaywallEventComponent() ||
        header?.stack?.containsPaywallEventComponent() == true ||
        stickyFooter?.stack?.containsPaywallEventComponent() == true

private fun StackComponent.containsPaywallEventComponent(): Boolean =
    components.any { it.containsPaywallEventComponent() }

@Suppress("CyclomaticComplexMethod")
private fun PaywallComponent.containsPaywallEventComponent(): Boolean = when (this) {
    is PackageComponent,
    is PurchaseButtonComponent,
    -> true

    is StackComponent -> containsPaywallEventComponent()
    is ButtonComponent -> stack.containsPaywallEventComponent() ||
        (action as? ButtonComponent.Action.NavigateTo)?.destination.let { destination ->
            when (destination) {
                is ButtonComponent.Destination.Sheet -> destination.stack.containsPaywallEventComponent()
                is ButtonComponent.Destination.CustomerCenter,
                is ButtonComponent.Destination.PrivacyPolicy,
                is ButtonComponent.Destination.Terms,
                is ButtonComponent.Destination.Url,
                is ButtonComponent.Destination.Unknown,
                null,
                -> false
            }
        }
    is HeaderComponent -> stack.containsPaywallEventComponent()
    is StickyFooterComponent -> stack.containsPaywallEventComponent()
    is CarouselComponent -> pages.any { it.containsPaywallEventComponent() }
    is TabsComponent -> tabs.any { it.stack.containsPaywallEventComponent() } ||
        control.let {
            when (it) {
                is TabsComponent.TabControl.Buttons -> it.stack.containsPaywallEventComponent()
                is TabsComponent.TabControl.Toggle -> it.stack.containsPaywallEventComponent()
            }
        }
    is CountdownComponent -> countdownStack.containsPaywallEventComponent() ||
        endStack?.containsPaywallEventComponent() == true ||
        fallback?.containsPaywallEventComponent() == true
    is TabControlButtonComponent -> stack.containsPaywallEventComponent()
    is TextComponent,
    is ImageComponent,
    is VideoComponent,
    is IconComponent,
    is TimelineComponent,
    is TabControlToggleComponent,
    is TabControlComponent,
    is FallbackHeaderComponent,
    -> false
}
