@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedWebViewPartial
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

@Immutable
internal data class WebViewComponentStyle(
    val url: String,
    override val visible: Boolean,
    override val size: Size,
    /** Schema `web_view.id`, sent to the content during the handshake. A blank id renders nothing. */
    val componentId: String,
    val overrides: List<PresentedOverride<PresentedWebViewPartial>>,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    override val rcPackage: Package?,
    /**
     * The resolved offer for this package, containing the subscription option and promo offer status.
     * Used to determine offer eligibility and pricing phase information.
     */
    @get:JvmSynthetic
    override val resolvedOffer: ResolvedOffer? = null,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for web views inside tab control elements. Not for all web views within a
     * tab.
     */
    @get:JvmSynthetic
    override val tabIndex: Int?,
    /**
     * The pre-computed offer eligibility for this component's package context.
     * Used for applying conditional overrides based on intro/promo offer status.
     * Null if this component is not in a package scope.
     */
    @get:JvmSynthetic
    override val offerEligibility: OfferEligibility? = null,
    val ignoreTopWindowInsets: Boolean = false,
) : ComponentStyle, PackageContext
