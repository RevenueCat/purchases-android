package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.ArrayRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Represents a Google font provider.
 */
@Parcelize
@Poko
@Immutable
public class GoogleFontProvider(
    /**
     * The resource ID of the font provider's certificate(s).
     */
    @ArrayRes public val certificates: Int,
    public val providerAuthority: String = "com.google.android.gms.fonts",
    public val providerPackage: String = "com.google.android.gms",
) : Parcelable {

    public fun toGoogleProvider(): GoogleFont.Provider {
        return GoogleFont.Provider(
            providerAuthority,
            providerPackage,
            certificates,
        )
    }
}
