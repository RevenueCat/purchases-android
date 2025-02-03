package com.revenuecat.purchases.customercenter.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CustomerCenterBackendEvent(
    @get:JvmSynthetic
    val id: String,

    @get:JvmSynthetic
    @SerialName("revision_id")
    val revisionID: Int,

    @get:JvmSynthetic
    val type: String,

    @get:JvmSynthetic
    @SerialName("app_user_id")
    val appUserID: String,

    @get:JvmSynthetic
    @SerialName("session_id")
    val appSessionID: String,

    @get:JvmSynthetic
    val timestamp: Long,

    @get:JvmSynthetic
    @SerialName("dark_mode")
    val darkMode: Boolean,

    @get:JvmSynthetic
    val locale: String,

    @get:JvmSynthetic
    @SerialName("is_sandbox")
    val isSandbox: Boolean,

    @get:JvmSynthetic
    @SerialName("display_mode")
    val displayMode: String,
)
