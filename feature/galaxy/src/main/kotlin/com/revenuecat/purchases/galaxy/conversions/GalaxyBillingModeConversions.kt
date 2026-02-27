package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

internal fun GalaxyBillingMode.toSamsungIAPOperationMode(): HelperDefine.OperationMode {
    return when (this) {
        GalaxyBillingMode.PRODUCTION -> HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION
        GalaxyBillingMode.TEST -> HelperDefine.OperationMode.OPERATION_MODE_TEST
        GalaxyBillingMode.ALWAYS_FAIL -> HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE
    }
}
