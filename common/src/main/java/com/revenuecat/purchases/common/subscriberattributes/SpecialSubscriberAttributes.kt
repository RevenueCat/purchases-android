package com.revenuecat.purchases.common.subscriberattributes

/**
 * Special Attributes
 */
const val SPECIAL_KEY_EMAIL = "\$email"
const val SPECIAL_KEY_DISPLAY_NAME = "\$displayName"
const val SPECIAL_KEY_PHONE_NUMBER = "\$phoneNumber"
const val SPECIAL_KEY_FCM_TOKENS = "\$fcmTokens"

/**
 * Device Identifiers
 */
const val SPECIAL_KEY_IDFA = "\$idfa"
const val SPECIAL_KEY_IDFV = "\$idfv"
const val SPECIAL_KEY_IP = "\$ip"
const val SPECIAL_KEY_GPS_AD_ID = "\$gpsAdId"
const val SPECIAL_KEY_ANDROID_ID = "\$androidId"

/**
 * Attribution IDs
 */
const val SPECIAL_KEY_ADJUST_ID = "\$adjustId"
const val SPECIAL_KEY_APPSFLYER_ID = "\$appsflyerId"
const val SPECIAL_KEY_FB_ANON_ID = "\$fbAnonId"
const val SPECIAL_KEY_MPARTICLE_ID = "\$mparticleId"
const val SPECIAL_KEY_ONESIGNAL_ID = "\$onesignalId"

/**
 * Optional campaign parameters
 */
const val SPECIAL_KEY_MEDIA_SOURCE = "\$mediaSource"
const val SPECIAL_KEY_CAMPAIGN = "\$campaign"
const val SPECIAL_KEY_AD_GROUP = "\$adGroup"
const val SPECIAL_KEY_AD = "\$ad"
const val SPECIAL_KEY_KEYWORD = "\$keyword"
const val SPECIAL_KEY_CREATIVE = "\$creative"

sealed class SubscriberAttributeKey(val backendKey: String) {

    object Email : SubscriberAttributeKey(SPECIAL_KEY_EMAIL)
    object DisplayName : SubscriberAttributeKey(SPECIAL_KEY_DISPLAY_NAME)
    object PhoneNumber : SubscriberAttributeKey(SPECIAL_KEY_PHONE_NUMBER)
    object FCMTokens : SubscriberAttributeKey(SPECIAL_KEY_FCM_TOKENS)
    class Custom(value: String) : SubscriberAttributeKey(value)

    sealed class DeviceIdentifiers {
        object GPSAdID : SubscriberAttributeKey(SPECIAL_KEY_GPS_AD_ID)
        object AndroidID : SubscriberAttributeKey(SPECIAL_KEY_ANDROID_ID)
        object IP : SubscriberAttributeKey(SPECIAL_KEY_IP)
    }

    sealed class AttributionIds(backendKey: String) : SubscriberAttributeKey(backendKey) {
        object Adjust : AttributionIds(SPECIAL_KEY_ADJUST_ID)
        object AppsFlyer : AttributionIds(SPECIAL_KEY_APPSFLYER_ID)
        object Facebook : AttributionIds(SPECIAL_KEY_FB_ANON_ID)
        object Mparticle : AttributionIds(SPECIAL_KEY_MPARTICLE_ID)
        object OneSignal : AttributionIds(SPECIAL_KEY_ONESIGNAL_ID)
    }

    sealed class CampaignParameters(backendKey: String) : SubscriberAttributeKey(backendKey) {
        object MediaSource : CampaignParameters(SPECIAL_KEY_MEDIA_SOURCE)
        object Campaign : CampaignParameters(SPECIAL_KEY_CAMPAIGN)
        object AdGroup : CampaignParameters(SPECIAL_KEY_AD_GROUP)
        object Ad : CampaignParameters(SPECIAL_KEY_AD)
        object Keyword : CampaignParameters(SPECIAL_KEY_KEYWORD)
        object Creative : CampaignParameters(SPECIAL_KEY_CREATIVE)
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
    SPECIAL_KEY_EMAIL -> SubscriberAttributeKey.Email
    SPECIAL_KEY_DISPLAY_NAME -> SubscriberAttributeKey.DisplayName
    SPECIAL_KEY_PHONE_NUMBER -> SubscriberAttributeKey.PhoneNumber
    SPECIAL_KEY_FCM_TOKENS -> SubscriberAttributeKey.FCMTokens
    else -> SubscriberAttributeKey.Custom(this)
}
