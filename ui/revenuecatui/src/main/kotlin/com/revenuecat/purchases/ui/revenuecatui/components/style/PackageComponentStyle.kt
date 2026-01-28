package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

@Immutable
internal data class PackageComponentStyle(
    @get:JvmSynthetic
    val rcPackage: Package,
    @get:JvmSynthetic
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val isSelectable: Boolean,
    /**
     * The resolved Play Store offer for this package, if configured.
     * Used for purchase flow and template variables.
     */
    @get:JvmSynthetic
    val resolvedOffer: ResolvedOffer? = null,
) : ComponentStyle {
    override val visible: Boolean = stackComponentStyle.visible
    override val size: Size = stackComponentStyle.size

    /**
     * Unique identifier for this package component, combining package ID and offer ID.
     * This allows distinguishing between multiple components that reference the same package
     * but with different offer configurations.
     */
    @get:JvmSynthetic
    val uniqueId: String
        get() {
            val offerId = (resolvedOffer as? ResolvedOffer.ConfiguredOffer)
                ?.option
                ?.id
            return if (offerId != null) {
                "${rcPackage.identifier}:$offerId"
            } else {
                rcPackage.identifier
            }
        }
}
