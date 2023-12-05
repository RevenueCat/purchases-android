package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
internal class PaywallViewAttributesReader {
    companion object {
        private enum class Attributes {
            OfferingId,
            ShouldDisplayDismissButton,
            FontFamily,
        }
        private val styleablesByStyleSet: Map<IntArray, Map<Attributes, Int>> = mapOf(
            R.styleable.PaywallView to mapOf(
                Attributes.OfferingId to R.styleable.PaywallView_offeringIdentifier,
                Attributes.ShouldDisplayDismissButton to R.styleable.PaywallView_shouldDisplayDismissButton,
                Attributes.FontFamily to R.styleable.PaywallView_android_fontFamily,
            ),
            R.styleable.PaywallFooterView to mapOf(
                Attributes.OfferingId to R.styleable.PaywallFooterView_offeringIdentifier,
                Attributes.FontFamily to R.styleable.PaywallFooterView_android_fontFamily,
            ),
        )

        @Suppress("ReturnCount", "NestedBlockDepth")
        fun parseAttributes(context: Context, attrsSet: AttributeSet?, styleAttrs: IntArray): PaywallViewAttributes? {
            if (attrsSet == null) {
                return null
            }
            var fontFamilyId: Int? = null
            var offeringIdentifier: String? = null
            var shouldDisplayDismissButton: Boolean? = null
            context.obtainStyledAttributes(
                attrsSet,
                styleAttrs,
                0,
                0,
            ).apply {
                try {
                    val styleables = styleablesByStyleSet[styleAttrs] ?: run {
                        Logger.e("Styleable not found for PaywallView")
                        return null
                    }
                    fontFamilyId = styleables[Attributes.FontFamily]?.let { getResourceId(it, 0) }
                    offeringIdentifier = styleables[Attributes.OfferingId]?.let { getString(it) }
                    styleables[Attributes.ShouldDisplayDismissButton]?.let {
                        shouldDisplayDismissButton = if (hasValue(it)) {
                            getBoolean(it, false)
                        } else {
                            null
                        }
                    }
                } finally {
                    recycle()
                }
            }
            val fontFamily = fontFamilyId?.takeIf { it > 0 }?.let {
                val typeface = ResourcesCompat.getFont(context, it)
                if (typeface == null) {
                    Logger.e("Font given for PaywallView not found")
                    null
                } else {
                    CustomFontProvider(FontFamily(typeface))
                }
            }
            return PaywallViewAttributes(
                offeringId = offeringIdentifier,
                fontProvider = fontFamily,
                shouldDisplayDismissButton = shouldDisplayDismissButton,
            )
        }
    }

    data class PaywallViewAttributes(
        val offeringId: String?,
        val fontProvider: FontProvider?,
        val shouldDisplayDismissButton: Boolean?,
    )
}
