package com.revenuecat.purchases.parceler

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.parceler.JSONObjectParceler.write
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JSONObjectParcelerTest {
    @Test
    fun `JSONObject is parcelable`() {
        val expected = JSONObject(Responses.validFullPurchaserResponse)

        val parcel = Parcel.obtain()
        expected.write(parcel, 0)
        parcel.setDataPosition(0)

        val created = JSONObjectParceler.create(parcel)

        assertThat(expected.toString()).isEqualTo(created.toString())
    }
}
