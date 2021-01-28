package com.revenuecat.purchases.parceler

import android.os.Parcel
import kotlinx.android.parcel.Parceler
import org.json.JSONObject

/** @suppress */
internal object JSONObjectParceler : Parceler<JSONObject> {

    override fun create(parcel: Parcel): JSONObject {
        return JSONObject(parcel.readString())
    }

    override fun JSONObject.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.toString())
    }
}
