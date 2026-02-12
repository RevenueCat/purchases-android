package com.revenuecat.purchases.utils

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame

inline fun <reified T : Parcelable> testParcelization(value: T?, areSame: Boolean = false) {
    val key = "key"

    // We don't have a good way to access the CREATOR (especially if the
    // tested Parcelable uses @Parcelize annotation), so we have to wrap
    // it in a bundle.
    val inputBundle = Bundle()
    inputBundle.putParcelable(key, value)

    val parcel = Parcel.obtain()
    try {
        inputBundle.writeToParcel(parcel, 0)

        // rewind the parcel to be ready to be read
        parcel.setDataPosition(0)

        // extract the bundle and a wrapped parcelable
        val outputBundle = parcel.readBundle()!!
        outputBundle.classLoader = T::class.java.classLoader
        val outputValue = outputBundle.getParcelable<T>(key)

        // assert that the parcelization succeeded
        assertNotSame(inputBundle, outputBundle)
        if (!areSame) {
            assertNotSame(value, outputValue)
            assertEquals(value, outputValue)
        } else {
            assertSame(value, outputValue)
        }
    } finally {
        parcel.recycle()
    }
}
