package com.revenuecat.purchases.attributes

internal const val SPECIAL_KEY_EMAIL = "\$email"
internal const val SPECIAL_KEY_DISPLAY_NAME = "\$displayName"
internal const val SPECIAL_KEY_PHONE_NUMBER = "\$phoneNumber"
internal const val SPECIAL_KEY_FCM_TOKENS = "\$fcmTokens"

internal sealed class SubscriberAttributeKey(val backendKey: String) {

    object Email : SubscriberAttributeKey(SPECIAL_KEY_EMAIL)
    object DisplayName : SubscriberAttributeKey(SPECIAL_KEY_DISPLAY_NAME)
    object PhoneNumber : SubscriberAttributeKey(SPECIAL_KEY_PHONE_NUMBER)
    object FCMTokens : SubscriberAttributeKey(SPECIAL_KEY_FCM_TOKENS)
    class Custom(value: String) : SubscriberAttributeKey(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriberAttributeKey

        if (backendKey != other.backendKey) return false

        return true
    }

    override fun hashCode(): Int {
        return backendKey.hashCode()
    }
}

internal fun String.getSubscriberAttributeKey(): SubscriberAttributeKey = when (this) {
    SPECIAL_KEY_EMAIL -> SubscriberAttributeKey.Email
    SPECIAL_KEY_DISPLAY_NAME -> SubscriberAttributeKey.DisplayName
    SPECIAL_KEY_PHONE_NUMBER -> SubscriberAttributeKey.PhoneNumber
    SPECIAL_KEY_FCM_TOKENS -> SubscriberAttributeKey.FCMTokens
    else -> SubscriberAttributeKey.Custom(this)
}