package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
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

/**
 * Returns all PaywallComponent that satisfy the predicate.
 *
 * Implemented as breadth-first search.
 */
@OptIn(InternalRevenueCatAPI::class)
@Suppress("CyclomaticComplexMethod")
internal fun PaywallComponent.filter(predicate: (PaywallComponent) -> Boolean): List<PaywallComponent> {
    val matches = mutableListOf<PaywallComponent>()
    val queue = ArrayDeque<PaywallComponent>()
    queue.add(this)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()

        if (predicate(current)) {
            matches.add(current)
        }

        when (current) {
            is StackComponent -> queue.addAll(current.components)
            is PurchaseButtonComponent -> queue.add(current.stack)
            is ButtonComponent -> queue.add(current.stack)
            is PackageComponent -> queue.add(current.stack)
            is StickyFooterComponent -> queue.add(current.stack)
            is CarouselComponent -> queue.addAll(current.pages)
            is TabControlButtonComponent -> queue.add(current.stack)
            is TabsComponent -> {
                when (val control = current.control) {
                    is TabsComponent.TabControl.Buttons -> queue.add(control.stack)
                    is TabsComponent.TabControl.Toggle -> queue.add(control.stack)
                }
                queue.addAll(current.tabs.map { it.stack })
            }

            is TimelineComponent -> {
                queue.addAll(current.items.flatMap { listOfNotNull(it.title, it.description, it.icon) })
            }

            is CountdownComponent -> {
                queue.add(current.countdownStack)
                current.endStack?.let { queue.add(it) }
                current.fallback?.let { queue.add(it) }
            }

            is VideoComponent,
            is TabControlToggleComponent,
            is TabControlComponent,
            is ImageComponent,
            is IconComponent,
            is TextComponent,
            -> {
                // These don't have child components.
            }
        }
    }

    return matches
}
