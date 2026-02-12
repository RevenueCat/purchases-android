package com.revenuecat.purchases.common

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.VerificationResult
import org.json.JSONObject
import java.util.Date

fun createCustomerInfo(
    response: String,
    requestDate: Date? = null,
    verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED
): CustomerInfo {
    return createCustomerInfo(JSONObject(response), requestDate, verificationResult)
}

fun createCustomerInfo(
    jsonObject: JSONObject,
    requestDate: Date? = null,
    verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED
): CustomerInfo {
    return CustomerInfoFactory.buildCustomerInfo(jsonObject, requestDate, verificationResult)
}
