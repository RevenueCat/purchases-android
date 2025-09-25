@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
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
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository

internal class OfferingVideoPredownloader(
    context: Context,
    private val fileRepository: FileRepository = DefaultFileRepository(context),
) {
    private val shouldPredownload: Boolean = try {
        Class.forName("com.revenuecat.purchases.ui.revenuecatui.PaywallKt")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    fun downloadVideos(offering: Offering) {
        if (shouldPredownload) {
            offering.paywallComponents?.data?.componentsConfig?.base?.stack
                ?.filter { it is VideoComponent }
                ?.forEach { component ->
                    if (component is VideoComponent) {
                        val videos = setOfNotNull(
                            component.source.light.url,
                            component.source.light.urlLowRes,
                            component.source.dark?.url,
                            component.source.dark?.urlLowRes,
                        )
                        fileRepository.prefetch(videos.toList())
                    }
                }
        }
    }

    /**
     * Returns all PaywallComponent that satisfy the predicate.
     *
     * Implemented as breadth-first search.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun PaywallComponent.filter(predicate: (PaywallComponent) -> Boolean): List<PaywallComponent> {
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
}
