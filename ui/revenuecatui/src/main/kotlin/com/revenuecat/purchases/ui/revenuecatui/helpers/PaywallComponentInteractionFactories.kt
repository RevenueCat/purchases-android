@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData
import com.revenuecat.purchases.paywalls.events.PaywallComponentType

@InternalRevenueCatAPI
internal fun paywallTabControlButtonSelection(
    tabsComponentName: String?,
    destinationTabId: String,
    originIndex: Int?,
    destinationIndex: Int?,
    originContextName: String?,
    destinationContextName: String?,
    defaultIndex: Int?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.TAB,
    componentName = tabsComponentName,
    componentValue = destinationTabId,
    originIndex = originIndex,
    destinationIndex = destinationIndex,
    originContextName = originContextName,
    destinationContextName = destinationContextName,
    defaultIndex = defaultIndex,
)

@InternalRevenueCatAPI
internal fun paywallPurchaseButtonAction(
    componentName: String?,
    componentValue: String,
    componentUrl: String?,
    currentPackageIdentifier: String?,
    currentProductIdentifier: String?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.PURCHASE_BUTTON,
    componentName = componentName,
    componentValue = componentValue,
    componentUrl = componentUrl,
    currentPackageIdentifier = currentPackageIdentifier,
    currentProductIdentifier = currentProductIdentifier,
)

@InternalRevenueCatAPI
@Suppress("LongParameterList")
internal data class CarouselPageChangeInteraction(
    val componentName: String?,
    val destinationPageIndex: Int,
    val originPageIndex: Int,
    val defaultPageIndex: Int,
    val originContextName: String?,
    val destinationContextName: String?,
)

@InternalRevenueCatAPI
internal fun paywallCarouselPageChange(
    interaction: CarouselPageChangeInteraction,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.CAROUSEL,
    componentName = interaction.componentName,
    componentValue = interaction.destinationPageIndex.toString(),
    originIndex = interaction.originPageIndex,
    destinationIndex = interaction.destinationPageIndex,
    originContextName = interaction.originContextName,
    destinationContextName = interaction.destinationContextName,
    defaultIndex = interaction.defaultPageIndex,
)

@InternalRevenueCatAPI
internal fun paywallPackageSelectionSheetOpen(
    sheetComponentName: String?,
    rootSelectedPackage: Package?,
): PaywallComponentInteractionData = PaywallComponentInteractionData(
    componentType = PaywallComponentType.PACKAGE_SELECTION_SHEET,
    componentName = sheetComponentName,
    componentValue = "open",
    currentPackageIdentifier = rootSelectedPackage?.identifier,
    currentProductIdentifier = rootSelectedPackage?.product?.paywallProductIdentifier(),
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
    currentProductIdentifier = sheetSelectedPackage?.product?.paywallProductIdentifier(),
    resultingProductIdentifier = resultingRootPackage?.product?.paywallProductIdentifier(),
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
    originProductIdentifier = origin?.product?.paywallProductIdentifier(),
    destinationProductIdentifier = destination.product.paywallProductIdentifier(),
    defaultProductIdentifier = defaultPackage?.product?.paywallProductIdentifier(),
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
    originProductIdentifier = originPackage?.product?.paywallProductIdentifier(),
    destinationProductIdentifier = destinationPackage?.product?.paywallProductIdentifier(),
)
