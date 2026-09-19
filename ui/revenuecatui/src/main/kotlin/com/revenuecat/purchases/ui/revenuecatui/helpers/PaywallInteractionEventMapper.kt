@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent.ComponentTypes
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent.Keys

internal fun PaywallEvent.toInteractionEvent(): PaywallInteractionEvent {
    val interaction = requireNotNull(componentInteraction) { "Not a component interaction event: $type" }
    val rawProperties = buildMap {
        put(Keys.TIMESTAMP.name, creationData.date.time)
        put(Keys.SESSION_ID.name, data.sessionIdentifier.toString())
        put(Keys.OFFERING_ID.name, data.presentedOfferingContext.offeringIdentifier)
        putIfNotNull(Keys.PAYWALL_ID.name, data.paywallIdentifier)
        put(Keys.PAYWALL_REVISION.name, data.paywallRevision)
        put(Keys.DISPLAY_MODE.name, data.displayMode)
        put(Keys.DARK_MODE.name, data.darkMode)
        put(Keys.LOCALE.name, data.localeIdentifier)

        put(Keys.COMPONENT_TYPE.name, interaction.componentType.toInteractionEventValue())
        put(Keys.COMPONENT_VALUE.name, interaction.componentValue)
        putIfNotNull(Keys.COMPONENT_NAME.name, interaction.componentName)
        putIfNotNull(Keys.COMPONENT_URL.name, interaction.componentUrl)
        putIfNotNull(Keys.ORIGIN_INDEX.name, interaction.originIndex)
        putIfNotNull(Keys.DESTINATION_INDEX.name, interaction.destinationIndex)
        putIfNotNull(Keys.ORIGIN_CONTEXT_NAME.name, interaction.originContextName)
        putIfNotNull(Keys.DESTINATION_CONTEXT_NAME.name, interaction.destinationContextName)
        putIfNotNull(Keys.DEFAULT_INDEX.name, interaction.defaultIndex)
        putIfNotNull(Keys.ORIGIN_PACKAGE_ID.name, interaction.originPackageIdentifier)
        putIfNotNull(Keys.DESTINATION_PACKAGE_ID.name, interaction.destinationPackageIdentifier)
        putIfNotNull(Keys.DEFAULT_PACKAGE_ID.name, interaction.defaultPackageIdentifier)
        putIfNotNull(Keys.CURRENT_PACKAGE_ID.name, interaction.currentPackageIdentifier)
        putIfNotNull(Keys.RESULTING_PACKAGE_ID.name, interaction.resultingPackageIdentifier)
        putIfNotNull(Keys.ORIGIN_PRODUCT_ID.name, interaction.originProductIdentifier)
        putIfNotNull(Keys.DESTINATION_PRODUCT_ID.name, interaction.destinationProductIdentifier)
        putIfNotNull(Keys.DEFAULT_PRODUCT_ID.name, interaction.defaultProductIdentifier)
        putIfNotNull(Keys.CURRENT_PRODUCT_ID.name, interaction.currentProductIdentifier)
        putIfNotNull(Keys.RESULTING_PRODUCT_ID.name, interaction.resultingProductIdentifier)
    }
    return PaywallInteractionEvent(rawProperties)
}

internal fun PaywallComponentType.toInteractionEventValue(): String = when (this) {
    PaywallComponentType.TAB -> ComponentTypes.TAB
    PaywallComponentType.SWITCH -> ComponentTypes.SWITCH
    PaywallComponentType.CAROUSEL -> ComponentTypes.CAROUSEL
    PaywallComponentType.BUTTON -> ComponentTypes.BUTTON
    PaywallComponentType.TEXT -> ComponentTypes.TEXT
    PaywallComponentType.PACKAGE -> ComponentTypes.PACKAGE
    PaywallComponentType.PACKAGE_SELECTION_SHEET -> ComponentTypes.PACKAGE_SELECTION_SHEET
    PaywallComponentType.PURCHASE_BUTTON -> ComponentTypes.PURCHASE_BUTTON
}

private fun MutableMap<String, Any>.putIfNotNull(key: String, value: Any?) {
    if (value != null) put(key, value)
}
