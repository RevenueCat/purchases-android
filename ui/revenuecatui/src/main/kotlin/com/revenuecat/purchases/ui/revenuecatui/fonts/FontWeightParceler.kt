package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcel
import androidx.compose.ui.text.font.FontWeight
import kotlinx.parcelize.Parceler

internal object FontWeightParceler : Parceler<FontWeight> {
    override fun create(parcel: Parcel) = FontWeight(parcel.readInt())

    override fun FontWeight.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(weight)
    }
}
