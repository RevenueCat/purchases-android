package com.revenuecat.purchases.galaxy.utils

import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.samsung.android.sdk.iap.lib.vo.ErrorVo

internal fun ErrorVo.isError(): Boolean {
    return this.errorCode != GalaxyErrorCode.IAP_ERROR_NONE.code
}
