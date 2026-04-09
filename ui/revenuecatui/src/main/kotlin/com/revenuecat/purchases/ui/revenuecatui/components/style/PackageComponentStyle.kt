package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPackagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

@Suppress("LongParameterList")
@Immutable
internal data class PackageComponentStyle(
    @get:JvmSynthetic
    override val rcPackage: Package,
    @get:JvmSynthetic
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val isSelectable: Boolean,
    @get:JvmSynthetic
    override val resolvedOffer: ResolvedOffer? = null,
    @get:JvmSynthetic
    override val visible: Boolean,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedPackagePartial>>,
    /**
     * Pre-computed offer eligibility for this package, used for evaluating intro/promo offer conditions
     * in package-level overrides.
     */
    @get:JvmSynthetic
    override val offerEligibility: OfferEligibility? = null,
) : ComponentStyle, PackageContext {
    override val size: Size = stackComponentStyle.size

    @get:JvmSynthetic
    override val tabIndex: Int? = null

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
