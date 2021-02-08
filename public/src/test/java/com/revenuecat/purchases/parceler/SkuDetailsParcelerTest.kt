package com.revenuecat.purchases.parceler

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.parceler.SkuDetailsParceler.write
import com.revenuecat.purchases.utils.stubSkuDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkuDetailsParcelerTest {
    @Test
    fun `SkuDetails is parcelable`() {
        val expected = stubSkuDetails()

        val parcel = Parcel.obtain()
        expected.write(parcel, 0)
        parcel.setDataPosition(0)

        val created = SkuDetailsParceler.create(parcel)

        assertThat(expected).isEqualTo(created)
    }
}
