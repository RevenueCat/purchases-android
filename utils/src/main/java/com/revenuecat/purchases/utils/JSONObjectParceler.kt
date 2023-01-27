package com.revenuecat.purchases.utils

import android.os.Parcel
import kotlinx.parcelize.Parceler
import org.json.JSONObject

/** @suppress */
object JSONObjectParceler : Parceler<JSONObject> {

    override fun create(parcel: Parcel): JSONObject {
        return JSONObject(parcel.readString())
    }

    override fun JSONObject.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.toString())
    }
}
