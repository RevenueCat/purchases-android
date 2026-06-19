package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

@Throws(PurchasesException::class)
internal fun GalaxyBillingMode.toSamsungIAPOperationMode(): HelperDefine.OperationMode {
    return when (this) {
        GalaxyBillingMode.PRODUCTION -> HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION
        GalaxyBillingMode.TEST -> HelperDefine.OperationMode.OPERATION_MODE_TEST
        GalaxyBillingMode.ALWAYS_FAIL -> HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE
        else -> throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.UnsupportedError,
                "Unsupported GalaxyBillingMode: $this",
            ),
        )
    }
}
