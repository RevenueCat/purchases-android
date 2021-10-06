package com.revenuecat.purchases.common.subscriberattributes

enum class ReservedSubscriberAttribute(val value: String) {
    /**
     * Special Attributes
     */
    EMAIL("\$email"),
    DISPLAY_NAME("\$displayName"),
    PHONE_NUMBER("\$phoneNumber"),
    FCM_TOKENS("\$fcmTokens"),

    /**
     * Device Identifiers
     */
    IDFA("\$idfa"),
    IDFV("\$idfv"),
    IP("\$ip"),
    GPS_AD_ID("\$gpsAdId"),
    ANDROID_ID("\$androidId"),
    AMAZON_AD_ID("\$amazonAdId"),

    /**
     * Attribution IDs
     */
    ADJUST_ID("\$adjustId"),
    APPSFLYER_ID("\$appsflyerId"),
    FB_ANON_ID("\$fbAnonId"),
    MPARTICLE_ID("\$mparticleId"),
    ONESIGNAL_ID("\$onesignalId"),

    /**
     * Optional campaign parameters
     */
    MEDIA_SOURCE("\$mediaSource"),
    CAMPAIGN("\$campaign"),
    AD_GROUP("\$adGroup"),
    AD("\$ad"),
    KEYWORD("\$keyword"),
    CREATIVE("\$creative"),
}

sealed class SubscriberAttributeKey(val backendKey: String) {

    object Email : SubscriberAttributeKey(ReservedSubscriberAttribute.EMAIL.value)
    object DisplayName : SubscriberAttributeKey(ReservedSubscriberAttribute.DISPLAY_NAME.value)
    object PhoneNumber : SubscriberAttributeKey(ReservedSubscriberAttribute.PHONE_NUMBER.value)
    object FCMTokens : SubscriberAttributeKey(ReservedSubscriberAttribute.FCM_TOKENS.value)
    class Custom(value: String) : SubscriberAttributeKey(value)

    sealed class DeviceIdentifiers {
        object GPSAdID : SubscriberAttributeKey(ReservedSubscriberAttribute.GPS_AD_ID.value)
        object AndroidID : SubscriberAttributeKey(ReservedSubscriberAttribute.ANDROID_ID.value)
        object IP : SubscriberAttributeKey(ReservedSubscriberAttribute.IP.value)
        object AmazonAdID : SubscriberAttributeKey(ReservedSubscriberAttribute.AMAZON_AD_ID.value)
    }

    sealed class AttributionIds {
        object Adjust : SubscriberAttributeKey(ReservedSubscriberAttribute.ADJUST_ID.value)
        object AppsFlyer : SubscriberAttributeKey(ReservedSubscriberAttribute.APPSFLYER_ID.value)
        object Facebook : SubscriberAttributeKey(ReservedSubscriberAttribute.FB_ANON_ID.value)
        object Mparticle : SubscriberAttributeKey(ReservedSubscriberAttribute.MPARTICLE_ID.value)
        object OneSignal : SubscriberAttributeKey(ReservedSubscriberAttribute.ONESIGNAL_ID.value)
    }

    sealed class CampaignParameters {
        object MediaSource : SubscriberAttributeKey(ReservedSubscriberAttribute.MEDIA_SOURCE.value)
        object Campaign : SubscriberAttributeKey(ReservedSubscriberAttribute.CAMPAIGN.value)
        object AdGroup : SubscriberAttributeKey(ReservedSubscriberAttribute.AD_GROUP.value)
        object Ad : SubscriberAttributeKey(ReservedSubscriberAttribute.AD.value)
        object Keyword : SubscriberAttributeKey(ReservedSubscriberAttribute.KEYWORD.value)
        object Creative : SubscriberAttributeKey(ReservedSubscriberAttribute.CREATIVE.value)
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
    ReservedSubscriberAttribute.EMAIL.value -> SubscriberAttributeKey.Email
    ReservedSubscriberAttribute.DISPLAY_NAME.value -> SubscriberAttributeKey.DisplayName
    ReservedSubscriberAttribute.PHONE_NUMBER.value -> SubscriberAttributeKey.PhoneNumber
    ReservedSubscriberAttribute.FCM_TOKENS.value -> SubscriberAttributeKey.FCMTokens
    else -> SubscriberAttributeKey.Custom(this)
}
