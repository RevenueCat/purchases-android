package com.revenuecat.purchases.common.verification

class SignatureVerificationException(
    apiPath: String,
) : Exception("Failed signature verification for request with path $apiPath")
