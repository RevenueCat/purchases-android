package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.HTTPResult

fun HTTPResult.Companion.createResult(
    responseCode: Int = 200,
    payload: String = "{}",
    origin: HTTPResult.Origin = HTTPResult.Origin.BACKEND,
    verificationStatus: HTTPResult.VerificationStatus = HTTPResult.VerificationStatus.NOT_VERIFIED
) = HTTPResult(responseCode, payload, origin, verificationStatus)
