package com.revenuecat.purchases.common.subscriberattributes

enum class BackendKey(val value: String) {
    /**
     * Special Attributes
     */
    SPECIAL_KEY_EMAIL("\$email"),
    SPECIAL_KEY_DISPLAY_NAME("\$displayName"),
    SPECIAL_KEY_PHONE_NUMBER("\$phoneNumber"),
    SPECIAL_KEY_FCM_TOKENS("\$fcmTokens"),

    /**
     * Device Identifiers
     */
    SPECIAL_KEY_IDFA("\$idfa"),
    SPECIAL_KEY_IDFV("\$idfv"),
    SPECIAL_KEY_IP("\$ip"),
    SPECIAL_KEY_GPS_AD_ID("\$gpsAdId"),
    SPECIAL_KEY_ANDROID_ID("\$androidId"),
    SPECIAL_KEY_AMAZON_AD_ID("\$amazonAdId"),

    /**
     * Attribution IDs
     */
    SPECIAL_KEY_ADJUST_ID("\$adjustId"),
    SPECIAL_KEY_APPSFLYER_ID("\$appsflyerId"),
    SPECIAL_KEY_FB_ANON_ID("\$fbAnonId"),
    SPECIAL_KEY_MPARTICLE_ID("\$mparticleId"),
    SPECIAL_KEY_ONESIGNAL_ID("\$onesignalId"),

    /**
     * Optional campaign parameters
     */
    SPECIAL_KEY_MEDIA_SOURCE("\$mediaSource"),
    SPECIAL_KEY_CAMPAIGN("\$campaign"),
    SPECIAL_KEY_AD_GROUP("\$adGroup"),
    SPECIAL_KEY_AD("\$ad"),
    SPECIAL_KEY_KEYWORD("\$keyword"),
    SPECIAL_KEY_CREATIVE("\$creative"),
}

sealed class SubscriberAttributeKey(val backendKey: String) {

    object Email : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_EMAIL.value)
    object DisplayName : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_DISPLAY_NAME.value)
    object PhoneNumber : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_PHONE_NUMBER.value)
    object FCMTokens : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_FCM_TOKENS.value)
    class Custom(value: String) : SubscriberAttributeKey(value)

    sealed class DeviceIdentifiers {
        object GPSAdID : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_GPS_AD_ID.value)
        object AndroidID : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_ANDROID_ID.value)
        object IP : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_IP.value)
        object AmazonAdID : SubscriberAttributeKey(BackendKey.SPECIAL_KEY_AMAZON_AD_ID.value)
    }

    sealed class AttributionIds(backendKey: BackendKey) : SubscriberAttributeKey(backendKey.value) {
        object Adjust : AttributionIds(BackendKey.SPECIAL_KEY_ADJUST_ID)
        object AppsFlyer : AttributionIds(BackendKey.SPECIAL_KEY_APPSFLYER_ID)
        object Facebook : AttributionIds(BackendKey.SPECIAL_KEY_FB_ANON_ID)
        object Mparticle : AttributionIds(BackendKey.SPECIAL_KEY_MPARTICLE_ID)
        object OneSignal : AttributionIds(BackendKey.SPECIAL_KEY_ONESIGNAL_ID)
    }

    sealed class CampaignParameters(backendKey: BackendKey) : SubscriberAttributeKey(backendKey.value) {
        object MediaSource : CampaignParameters(BackendKey.SPECIAL_KEY_MEDIA_SOURCE)
        object Campaign : CampaignParameters(BackendKey.SPECIAL_KEY_CAMPAIGN)
        object AdGroup : CampaignParameters(BackendKey.SPECIAL_KEY_AD_GROUP)
        object Ad : CampaignParameters(BackendKey.SPECIAL_KEY_AD)
        object Keyword : CampaignParameters(BackendKey.SPECIAL_KEY_KEYWORD)
        object Creative : CampaignParameters(BackendKey.SPECIAL_KEY_CREATIVE)
    }

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

    override fun toString(): String {
        return "SubscriberAttributeKey('$backendKey')"
    }
}

fun String.getSubscriberAttributeKey(): SubscriberAttributeKey = when (this) {
    BackendKey.SPECIAL_KEY_EMAIL.value -> SubscriberAttributeKey.Email
    BackendKey.SPECIAL_KEY_DISPLAY_NAME.value -> SubscriberAttributeKey.DisplayName
    BackendKey.SPECIAL_KEY_PHONE_NUMBER.value -> SubscriberAttributeKey.PhoneNumber
    BackendKey.SPECIAL_KEY_FCM_TOKENS.value -> SubscriberAttributeKey.FCMTokens
    else -> SubscriberAttributeKey.Custom(this)
}
