package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import org.junit.Test
import kotlin.test.assertEquals

class BillingModeConversionsTest {

    @Test
    fun `production maps to production operation mode`() {
        assertEquals(
            HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION,
            GalaxyBillingMode.PRODUCTION.toSamsungOperationMode(),
        )
    }

    @Test
    fun `test maps to test operation mode`() {
        assertEquals(
            HelperDefine.OperationMode.OPERATION_MODE_TEST,
            GalaxyBillingMode.TEST.toSamsungOperationMode(),
        )
    }

    @Test
    fun `always fail maps to test failure operation mode`() {
        assertEquals(
            HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE,
            GalaxyBillingMode.ALWAYS_FAIL.toSamsungOperationMode(),
        )
    }
}
