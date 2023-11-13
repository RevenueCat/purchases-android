package com.revenuecat.purchases.common.subscriberattributes

internal enum class ReservedSubscriberAttribute(val value: String) {
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
    AIRSHIP_CHANNEL_ID("\$airshipChannelId"),
    CLEVER_TAP_ID("\$clevertapId"),

    /**
     * Integration IDs
     */
    MIXPANEL_DISTINCT_ID("\$mixpanelDistinctId"),
    FIREBASE_APP_INSTANCE_ID("\$firebaseAppInstanceId"),

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

internal sealed class SubscriberAttributeKey(val backendKey: String) {

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

    sealed class AttributionIds(backendKey: ReservedSubscriberAttribute) : SubscriberAttributeKey(backendKey.value) {
        object Adjust : AttributionIds(ReservedSubscriberAttribute.ADJUST_ID)
        object AppsFlyer : AttributionIds(ReservedSubscriberAttribute.APPSFLYER_ID)
        object Facebook : AttributionIds(ReservedSubscriberAttribute.FB_ANON_ID)
        object Mparticle : AttributionIds(ReservedSubscriberAttribute.MPARTICLE_ID)
        object CleverTap : AttributionIds(ReservedSubscriberAttribute.CLEVER_TAP_ID)
    }

    sealed class IntegrationIds(backendKey: ReservedSubscriberAttribute) : SubscriberAttributeKey(backendKey.value) {
        object MixpanelDistinctId : IntegrationIds(ReservedSubscriberAttribute.MIXPANEL_DISTINCT_ID)
        object OneSignal : IntegrationIds(ReservedSubscriberAttribute.ONESIGNAL_ID)
        object Airship : IntegrationIds(ReservedSubscriberAttribute.AIRSHIP_CHANNEL_ID)
        object FirebaseAppInstanceId : IntegrationIds(ReservedSubscriberAttribute.FIREBASE_APP_INSTANCE_ID)
    }

    sealed class CampaignParameters(
        backendKey: ReservedSubscriberAttribute,
    ) : SubscriberAttributeKey(backendKey.value) {
        object MediaSource : CampaignParameters(ReservedSubscriberAttribute.MEDIA_SOURCE)
        object Campaign : CampaignParameters(ReservedSubscriberAttribute.CAMPAIGN)
        object AdGroup : CampaignParameters(ReservedSubscriberAttribute.AD_GROUP)
        object Ad : CampaignParameters(ReservedSubscriberAttribute.AD)
        object Keyword : CampaignParameters(ReservedSubscriberAttribute.KEYWORD)
        object Creative : CampaignParameters(ReservedSubscriberAttribute.CREATIVE)
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

internal fun String.getSubscriberAttributeKey(): SubscriberAttributeKey = when (this) {
    ReservedSubscriberAttribute.EMAIL.value -> SubscriberAttributeKey.Email
    ReservedSubscriberAttribute.DISPLAY_NAME.value -> SubscriberAttributeKey.DisplayName
    ReservedSubscriberAttribute.PHONE_NUMBER.value -> SubscriberAttributeKey.PhoneNumber
    ReservedSubscriberAttribute.FCM_TOKENS.value -> SubscriberAttributeKey.FCMTokens
    else -> SubscriberAttributeKey.Custom(this)
}
