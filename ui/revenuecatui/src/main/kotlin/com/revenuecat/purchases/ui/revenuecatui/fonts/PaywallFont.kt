package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

sealed class PaywallFont : Parcelable {
    @Parcelize
    data class GoogleFont(
        val fontName: String,
        val fontProvider: GoogleFontProvider,
        @TypeParceler<FontWeight, FontWeightParceler>()
        val fontWeight: FontWeight = FontWeight.Normal,
        @TypeParceler<FontStyle, FontStyleParceler>()
        val fontStyle: FontStyle = FontStyle.Normal,
    ) : PaywallFont()

    @Parcelize
    data class ResourceFont(
        @FontRes
        val resourceId: Int,
        @TypeParceler<FontWeight, FontWeightParceler>()
        val fontWeight: FontWeight = FontWeight.Normal,
        @TypeParceler<FontStyle, FontStyleParceler>()
        val fontStyle: FontStyle = FontStyle.Normal,
    ) : PaywallFont()
}
