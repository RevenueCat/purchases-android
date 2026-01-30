package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import java.util.Date

@Suppress("LongParameterList")
@Immutable
internal class TextComponentStyle(
    @get:JvmSynthetic
    val texts: NonEmptyMap<LocaleId, String>,
    @get:JvmSynthetic
    val color: ColorStyles,
    @get:JvmSynthetic
    val fontSize: Int,
    @get:JvmSynthetic
    val fontWeight: FontWeight?,
    @get:JvmSynthetic
    val fontSpec: FontSpec?,
    @get:JvmSynthetic
    val textAlign: TextAlign?,
    @get:JvmSynthetic
    val horizontalAlignment: Alignment.Horizontal,
    @get:JvmSynthetic
    val backgroundColor: ColorStyles?,
    @get:JvmSynthetic
    override val visible: Boolean,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    /**
     * The package any variables in [texts] should take values from. The selected package will be used if this is null.
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    /**
     * Unique identifier for the package, combining package ID and offer ID.
     * Used for selection comparison when multiple components reference the same package with different offers.
     */
    @get:JvmSynthetic
    val packageUniqueId: String? = null,
    /**
     * The subscription option to use for offer variables (product.offer_*, product.secondary_offer_*).
     * When set, offer variables will use pricing phases from this option instead of defaultOption.
     * This is typically set when a specific Play Store offer is configured for the package.
     */
    @get:JvmSynthetic
    val subscriptionOption: SubscriptionOption? = null,
    /**
     * Whether this component uses a configured promo offer (Play Store offer ID).
     * Used to determine if the `promo_offer` override condition should apply.
     */
    @get:JvmSynthetic
    val isPromoOffer: Boolean = false,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for texts inside tab control elements. Not for all texts within a tab.
     */
    @get:JvmSynthetic
    val tabIndex: Int?,
    /**
     * If this is non-null, it means this text is inside a countdown component and countdown variables should be
     * replaced with values calculated from this date.
     */
    @get:JvmSynthetic
    val countdownDate: Date?,
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom,
    @get:JvmSynthetic
    val variableLocalizations: NonEmptyMap<LocaleId, NonEmptyMap<VariableLocalizationKey, String>>,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<LocalizedTextPartial>>,
) : ComponentStyle
