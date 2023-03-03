package com.revenuecat.purchases.common

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.VerificationResult
import org.json.JSONObject

fun createCustomerInfo(
    response: String,
    verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED
): CustomerInfo {
    return createCustomerInfo(JSONObject(response), verificationResult)
}

fun createCustomerInfo(
    jsonObject: JSONObject,
    verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED
): CustomerInfo {
    return CustomerInfoFactory.buildCustomerInfo(jsonObject, verificationResult)
}
