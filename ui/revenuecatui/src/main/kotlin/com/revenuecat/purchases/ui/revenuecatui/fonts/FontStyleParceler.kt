package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcel
import androidx.compose.ui.text.font.FontStyle
import kotlinx.parcelize.Parceler

internal object FontStyleParceler : Parceler<FontStyle> {
    override fun create(parcel: Parcel) = FontStyle(parcel.readInt())

    override fun FontStyle.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(value)
    }
}
