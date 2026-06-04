package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalaxyBillingModeConversionsTest {

    @Test
    fun `PRODUCTION maps to production mode`() {
        val operationMode = GalaxyBillingMode.PRODUCTION.toSamsungIAPOperationMode()

        assertThat(operationMode).isEqualTo(HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION)
    }

    @Test
    fun `TEST maps to test mode`() {
        val operationMode = GalaxyBillingMode.TEST.toSamsungIAPOperationMode()

        assertThat(operationMode).isEqualTo(HelperDefine.OperationMode.OPERATION_MODE_TEST)
    }

    @Test
    fun `ALWAYS_FAIL maps to test failure mode`() {
        val operationMode = GalaxyBillingMode.ALWAYS_FAIL.toSamsungIAPOperationMode()

        assertThat(operationMode).isEqualTo(HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE)
    }
}
