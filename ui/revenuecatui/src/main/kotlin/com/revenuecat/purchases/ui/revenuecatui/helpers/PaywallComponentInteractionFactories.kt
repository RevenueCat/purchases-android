@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData
import com.revenuecat.purchases.paywalls.events.PaywallComponentType

@InternalRevenueCatAPI
internal fun paywallPackageSelectionSheetOpen(
    sheetComponentName: String?,
    rootSelectedPackage: Package?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.PACKAGE_SELECTION_SHEET,
    componentName = sheetComponentName,
    componentValue = "open",
    currentPackageIdentifier = rootSelectedPackage?.identifier,
    currentProductIdentifier = rootSelectedPackage?.product?.id,
)

@InternalRevenueCatAPI
internal fun paywallPackageSelectionSheetClose(
    sheetComponentName: String?,
    sheetSelectedPackage: Package?,
    resultingRootPackage: Package?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.PACKAGE_SELECTION_SHEET,
    componentName = sheetComponentName,
    componentValue = "close",
    currentPackageIdentifier = sheetSelectedPackage?.identifier,
    resultingPackageIdentifier = resultingRootPackage?.identifier,
    currentProductIdentifier = sheetSelectedPackage?.product?.id,
    resultingProductIdentifier = resultingRootPackage?.product?.id,
)

@InternalRevenueCatAPI
internal fun paywallPackageRowSelection(
    componentName: String? = null,
    destination: Package,
    origin: Package?,
    defaultPackage: Package?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.PACKAGE,
    componentName = componentName,
    componentValue = destination.identifier,
    originPackageIdentifier = origin?.identifier,
    destinationPackageIdentifier = destination.identifier,
    defaultPackageIdentifier = defaultPackage?.identifier,
    originProductIdentifier = origin?.product?.id,
    destinationProductIdentifier = destination.product.id,
    defaultProductIdentifier = defaultPackage?.product?.id,
)

@InternalRevenueCatAPI
internal fun paywallTierSelection(
    tierDisplayName: String,
    componentName: String? = null,
    originPackage: Package?,
    destinationPackage: Package?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.TAB,
    componentName = componentName,
    componentValue = tierDisplayName,
    originPackageIdentifier = originPackage?.identifier,
    destinationPackageIdentifier = destinationPackage?.identifier,
    originProductIdentifier = originPackage?.product?.id,
    destinationProductIdentifier = destinationPackage?.product?.id,
)
