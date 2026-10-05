package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationResult
import java.util.Date

internal fun HTTPResult.Companion.createResult(
    responseCode: Int = RCHTTPStatusCodes.SUCCESS,
    payload: String = "{}",
    origin: HTTPResult.Origin = HTTPResult.Origin.BACKEND,
    requestDate: Date? = null,
    verificationResult: SignatureVerificationResult = SignatureVerificationResult.NotRequested,
    isLoadShedderResponse: Boolean = false,
    isFallbackURL: Boolean = false,
) = HTTPResult(responseCode, payload, origin, requestDate, verificationResult, isLoadShedderResponse, isFallbackURL)
