package com.revenuecat.sample.user

data class UserState(
    val isSubscriber: Boolean,
    val currentUserId: String,
    val displayErrorMessage: String? = null,
    val shouldStartLoginProcess: Boolean = false,
)
