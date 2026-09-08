package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCountdownPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import dev.drewhamilton.poko.Poko
import java.util.Date

@Suppress("LongParameterList")
@Poko
@Immutable
internal class CountdownComponentStyle(
    @get:JvmSynthetic
    val date: Date,
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom,
    @get:JvmSynthetic
    val countdownStackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val endStackComponentStyle: StackComponentStyle?,
    @get:JvmSynthetic
    val fallbackStackComponentStyle: StackComponentStyle?,
    override val visible: Boolean = true,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedCountdownPartial>> = emptyList(),
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    override val rcPackage: Package? = null,
    /**
     * The resolved offer for this package, containing the subscription option and promo offer status.
     * Used to determine offer eligibility and pricing phase information.
     */
    @get:JvmSynthetic
    override val resolvedOffer: ResolvedOffer? = null,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for countdowns inside tab control elements. Not for all countdowns within a
     * tab.
     */
    @get:JvmSynthetic
    override val tabIndex: Int? = null,
    /**
     * The pre-computed offer eligibility for this component's package context.
     * Used for applying conditional overrides based on intro/promo offer status.
     * Null if this component is not in a package scope.
     */
    @get:JvmSynthetic
    override val offerEligibility: OfferEligibility? = null,
) : ComponentStyle, PackageContext {
    override val size: Size = countdownStackComponentStyle.size
}
