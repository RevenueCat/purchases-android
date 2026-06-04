@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.templates

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPackageRowSelection

internal fun PaywallViewModel.trackTemplatePackageRowSelectionIfChanged(
    state: PaywallState.Loaded.Legacy,
    packageInfo: TemplateConfiguration.PackageInfo,
    defaultPackageInfo: TemplateConfiguration.PackageInfo = state.templateConfiguration.packages.default,
) {
    val origin = state.selectedPackage.value?.rcPackage
    val destination = packageInfo.rcPackage
    if (origin?.identifier == destination.identifier) {
        return
    }
    trackComponentInteraction(
        paywallPackageRowSelection(
            componentName = null,
            destination = destination,
            origin = origin,
            defaultPackage = defaultPackageInfo.rcPackage,
        ),
    )
}
