package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.annotation.ArrayRes
import androidx.compose.ui.text.googlefonts.GoogleFont
import kotlinx.parcelize.Parcelize

sealed class GoogleFontProvider(
    open val providerAuthority: String,
    open val providerPackage: String = "com.google.android.gms",
) : Parcelable {
    @Parcelize
    data class ResourceProvider(
        override val providerAuthority: String = "com.google.android.gms.fonts",
        override val providerPackage: String = "com.google.android.gms",
        @ArrayRes val certificates: Int,
    ) : GoogleFontProvider(providerAuthority, providerPackage)

    @Parcelize
    data class ByteArrayProvider(
        override val providerAuthority: String = "com.google.android.gms.fonts",
        override val providerPackage: String = "com.google.android.gms",
        val certificates: List<List<ByteArray>>,
    ) : GoogleFontProvider(providerAuthority, providerPackage)

    fun toGoogleProvider(): GoogleFont.Provider {
        return when (this) {
            is ResourceProvider -> GoogleFont.Provider(
                providerAuthority,
                providerPackage,
                certificates,
            )
            is ByteArrayProvider -> GoogleFont.Provider(
                providerAuthority,
                providerPackage,
                certificates,
            )
        }
    }
}
