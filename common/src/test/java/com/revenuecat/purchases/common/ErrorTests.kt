package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.junit.Test
import java.net.UnknownHostException

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ErrorTests {

    @Test
    fun testUnknownHostExceptionIsUnknownHostError() {
        assertThat(UnknownHostException().toPurchasesError().code).isEqualTo(PurchasesErrorCode.UnknownHostError)
    }
}