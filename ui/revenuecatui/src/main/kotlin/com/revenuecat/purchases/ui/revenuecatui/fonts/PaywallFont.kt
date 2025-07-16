package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Represents a font. You can create either a [GoogleFont] or a [ResourceFont].
 */
public sealed class PaywallFont : Parcelable {
    /**
     * Represents a downloadable Google Font.
     */
    @Parcelize
    @Poko
    public class GoogleFont(
        /**
         * Name of the Google font you want to use.
         */
        public val fontName: String,
        /**
         * Provider of the Google font.
         */
        public val fontProvider: GoogleFontProvider,
        /**
         * The weight of the font. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontWeight, FontWeightParceler>()
        public val fontWeight: FontWeight = FontWeight.Normal,
        /**
         * The style of the font, normal or italic. The system uses this to match a font to a font request.
         * We use int instead of [FontStyle] because [FontStyle] is not compatible with Java.
         */
        public val fontStyle: Int = FontStyle.Normal.value,
    ) : PaywallFont()

    @Parcelize
    @Poko
    public class ResourceFont(
        /**
         * The resource ID of the font file in font resources.
         */
        @FontRes
        public val resourceId: Int,
        /**
         * The weight of the font. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontWeight, FontWeightParceler>()
        public val fontWeight: FontWeight = FontWeight.Normal,
        /**
         * The style of the font, normal or italic. The system uses this to match a font to a font request.
         * We use int instead of [FontStyle] because [FontStyle] is not compatible with Java.
         */
        public val fontStyle: Int = FontStyle.Normal.value,
    ) : PaywallFont()

    @Parcelize
    @Poko
    public class AssetFont(
        /**
         * Full path starting from the assets directory (i.e. dir/myfont.ttf for assets/dir/myfont.ttf).
         */
        public val path: String,
        /**
         * The weight of the font. The system uses this to match a font to a font request.
         */
        @TypeParceler<FontWeight, FontWeightParceler>()
        public val fontWeight: FontWeight = FontWeight.Normal,
        /**
         * The style of the font, normal or italic. The system uses this to match a font to a font request.
         * We use int instead of [FontStyle] because [FontStyle] is not compatible with Java.
         */
        public val fontStyle: Int = FontStyle.Normal.value,
    ) : PaywallFont()
}
