package com.revenuecat.purchases.data

import com.revenuecat.purchases.CustomerInfo

/**
 * The result of a successful login operation. Used in coroutines.
 */
data class LogInResult(
    /**
     * The [CustomerInfo] associated with the logged in user.
     */
    val customerInfo: CustomerInfo,

    /**
     * true if a new user has been registered in the backend,
     * false if the user had already been registered.
     */
    val created: Boolean,
)
