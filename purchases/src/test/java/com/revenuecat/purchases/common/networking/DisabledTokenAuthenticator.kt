package com.revenuecat.purchases.common.networking

import io.mockk.mockk

/** A [TokenAuthenticator] with IAM disabled: requests keep the API key and the non-IAM paths. */
internal fun disabledTokenAuthenticator(): TokenAuthenticator =
    TokenAuthenticator(TokenManager(mockk(), "test_api_key", enabled = false)) { "" }
