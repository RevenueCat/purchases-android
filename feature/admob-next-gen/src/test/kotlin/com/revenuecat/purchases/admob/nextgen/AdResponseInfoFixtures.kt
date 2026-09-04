package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import io.mockk.every
import io.mockk.mockk

internal fun responseInfo(adapterClassName: String, responseId: String): ResponseInfo = mockk {
    every { this@mockk.adapterClassName } returns adapterClassName
    every { this@mockk.responseId } returns responseId
}
