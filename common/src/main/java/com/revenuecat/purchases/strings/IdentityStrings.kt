package com.revenuecat.purchases.strings

object IdentityStrings {
    const val LOGGING_IN = "Logging in from %s -> %s"
    const val LOG_IN_SUCCESSFUL = "Logged in successfully as %s. Created: %s"
    const val LOG_IN_ERROR_MISSING_APP_USER_ID = "Error logging in: appUserID can't be null, empty or blank"
    const val IDENTIFYING_APP_USER_ID = "Identifying App User ID: %s"
    const val EMPTY_APP_USER_ID_WILL_BECOME_ANONYMOUS = "Identifying with empty App User ID will be " +
        "treated as anonymous."
    const val SETTING_NEW_ANON_ID = "Setting new anonymous App User ID - %s"
    const val LOG_OUT_CALLED_ON_ANONYMOUS_USER = "Called logOut but the current user is anonymous"
    const val LOG_OUT_SUCCESSFUL = "Logged out successfully"
    const val INVALIDATING_CACHED_CUSTOMER_INFO = "Detected unverified cached CustomerInfo but verification " +
        "is enabled. Invalidating cache."
}
