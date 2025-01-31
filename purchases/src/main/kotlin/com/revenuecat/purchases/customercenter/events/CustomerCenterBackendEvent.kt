package com.revenuecat.purchases.paywalls.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerCenterBackendEvent(
    val id: String,
    val revisionId: Int,
    val type: String,
    @SerialName("app_user_id")
    val appUserID: String,
    @SerialName("session_id")
    val appSessionID: String,
    val timestamp: Long,
    @SerialName("dark_mode")
    val darkMode: Boolean,
    val locale: String,
    @SerialName("is_sandbox")
    val isSandbox: Boolean,
    @SerialName("display_mode")
    val displayMode: String,
)
