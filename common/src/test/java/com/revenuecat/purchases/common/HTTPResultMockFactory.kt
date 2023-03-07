package com.revenuecat.purchases.common

import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes

fun HTTPResult.Companion.createResult(
    responseCode: Int = RCHTTPStatusCodes.SUCCESS,
    payload: String = "{}",
    origin: HTTPResult.Origin = HTTPResult.Origin.BACKEND,
    verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED
) = HTTPResult(responseCode, payload, origin, verificationResult)
