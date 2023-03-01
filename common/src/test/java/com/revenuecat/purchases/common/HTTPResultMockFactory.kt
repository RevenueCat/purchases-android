package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes

fun HTTPResult.Companion.createResult(
    responseCode: Int = RCHTTPStatusCodes.SUCCESS,
    payload: String = "{}",
    origin: HTTPResult.Origin = HTTPResult.Origin.BACKEND,
    verificationStatus: HTTPResult.VerificationStatus = HTTPResult.VerificationStatus.NOT_VERIFIED
) = HTTPResult(responseCode, payload, origin, verificationStatus)
