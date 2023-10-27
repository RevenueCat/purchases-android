package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Represents a font. You can create either a [GoogleFont] or a [ResourceFont].
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
sealed class PaywallFont : Parcelable {
    /**
     * Represents a downloadable Google Font.
     */
    @Parcelize
    data class GoogleFont(
        /**
         * Name of the Google font you want to use.
         */
        val fontName: String,
        /**
         * Provider of the Google font.
         */
        val fontProvider: GoogleFontProvider,
        /**
         * The weight of the font. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontWeight, FontWeightParceler>()
        val fontWeight: FontWeight = FontWeight.Normal,
        /**
         * The style of the font, normal or italic. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontStyle, FontStyleParceler>()
        val fontStyle: FontStyle = FontStyle.Normal,
    ) : PaywallFont()

    @Parcelize
    data class ResourceFont(
        /**
         * The resource ID of the font file in font resources.
         */
        @FontRes
        val resourceId: Int,
        /**
         * The weight of the font. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontWeight, FontWeightParceler>()
        val fontWeight: FontWeight = FontWeight.Normal,
        /**
         * The style of the font, normal or italic. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontStyle, FontStyleParceler>()
        val fontStyle: FontStyle = FontStyle.Normal,
    ) : PaywallFont()
}
