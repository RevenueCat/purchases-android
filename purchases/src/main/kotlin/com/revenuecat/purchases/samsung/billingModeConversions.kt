package com.revenuecat.purchases.samsung

import com.samsung.android.sdk.iap.lib.constants.HelperDefine

internal fun SamsungBillingMode.toSamsungOperationMode(): HelperDefine.OperationMode {
    return when(this) {
        SamsungBillingMode.PRODUCTION -> HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION
        SamsungBillingMode.TEST -> HelperDefine.OperationMode.OPERATION_MODE_TEST
        SamsungBillingMode.ALWAYS_FAIL -> HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE
    }
}