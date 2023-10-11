package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.ArrayRes
import androidx.compose.ui.text.googlefonts.GoogleFont
import kotlinx.parcelize.Parcelize

/**
 * Represents a Google font provider.
 */
@Parcelize
data class GoogleFontProvider(
    /**
     * The resource ID of the font provider's certificate(s).
     */
    @ArrayRes val certificates: Int,
    val providerAuthority: String = "com.google.android.gms.fonts",
    val providerPackage: String = "com.google.android.gms",
) : Parcelable {

    fun toGoogleProvider(): GoogleFont.Provider {
        return GoogleFont.Provider(
            providerAuthority,
            providerPackage,
            certificates,
        )
    }
}
