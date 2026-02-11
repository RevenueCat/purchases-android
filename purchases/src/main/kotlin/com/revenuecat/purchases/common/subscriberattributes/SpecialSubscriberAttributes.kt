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
    DEVICE_VERSION("\$deviceVersion"),
    GPS_AD_ID("\$gpsAdId"),
    AMAZON_AD_ID("\$amazonAdId"),

    /**
     * Attribution IDs
     */
    ADJUST_ID("\$adjustId"),
    APPSFLYER_ID("\$appsflyerId"),
    FB_ANON_ID("\$fbAnonId"),
    MPARTICLE_ID("\$mparticleId"),
    ONESIGNAL_ID("\$onesignalId"),
    ONESIGNAL_USER_ID("\$onesignalUserId"),
    AIRSHIP_CHANNEL_ID("\$airshipChannelId"),
    CLEVER_TAP_ID("\$clevertapId"),
    KOCHAVA_DEVICE_ID("\$kochavaDeviceId"),
    AIRBRIDGE_DEVICE_ID("\$airbridgeDeviceId"),
    SOLAR_ENGINE_DISTINCT_ID("\$solarEngineDistinctId"),
    SOLAR_ENGINE_ACCOUNT_ID("\$solarEngineAccountId"),
    SOLAR_ENGINE_VISITOR_ID("\$solarEngineVisitorId"),

    /**
     * Integration IDs
     */
    MIXPANEL_DISTINCT_ID("\$mixpanelDistinctId"),
    FIREBASE_APP_INSTANCE_ID("\$firebaseAppInstanceId"),
    TENJIN_ANALYTICS_INSTALLATION_ID("\$tenjinId"),
    POSTHOG_USER_ID("\$posthogUserId"),

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

    public object Email : SubscriberAttributeKey(ReservedSubscriberAttribute.EMAIL.value)
    public object DisplayName : SubscriberAttributeKey(ReservedSubscriberAttribute.DISPLAY_NAME.value)
    public object PhoneNumber : SubscriberAttributeKey(ReservedSubscriberAttribute.PHONE_NUMBER.value)
    public object FCMTokens : SubscriberAttributeKey(ReservedSubscriberAttribute.FCM_TOKENS.value)
    public class Custom(value: String) : SubscriberAttributeKey(value)

    public sealed class DeviceIdentifiers {
        public object GPSAdID : SubscriberAttributeKey(ReservedSubscriberAttribute.GPS_AD_ID.value)
        public object IP : SubscriberAttributeKey(ReservedSubscriberAttribute.IP.value)
        public object DeviceVersion : SubscriberAttributeKey(ReservedSubscriberAttribute.DEVICE_VERSION.value)
        public object AmazonAdID : SubscriberAttributeKey(ReservedSubscriberAttribute.AMAZON_AD_ID.value)
    }

    public sealed class AttributionIds(backendKey: ReservedSubscriberAttribute) : SubscriberAttributeKey(backendKey.value) {
        public object Adjust : AttributionIds(ReservedSubscriberAttribute.ADJUST_ID)
        public object AppsFlyer : AttributionIds(ReservedSubscriberAttribute.APPSFLYER_ID)
        public object Facebook : AttributionIds(ReservedSubscriberAttribute.FB_ANON_ID)
        public object Mparticle : AttributionIds(ReservedSubscriberAttribute.MPARTICLE_ID)
        public object CleverTap : AttributionIds(ReservedSubscriberAttribute.CLEVER_TAP_ID)
        public object Kochava : AttributionIds(ReservedSubscriberAttribute.KOCHAVA_DEVICE_ID)
        public object Airbridge : AttributionIds(ReservedSubscriberAttribute.AIRBRIDGE_DEVICE_ID)
        public object SolarEngineDistinctId : AttributionIds(ReservedSubscriberAttribute.SOLAR_ENGINE_DISTINCT_ID)
        public object SolarEngineAccountId : AttributionIds(ReservedSubscriberAttribute.SOLAR_ENGINE_ACCOUNT_ID)
        public object SolarEngineVisitorId : AttributionIds(ReservedSubscriberAttribute.SOLAR_ENGINE_VISITOR_ID)
    }

    public sealed class IntegrationIds(backendKey: ReservedSubscriberAttribute) : SubscriberAttributeKey(backendKey.value) {
        public object MixpanelDistinctId : IntegrationIds(ReservedSubscriberAttribute.MIXPANEL_DISTINCT_ID)
        public object OneSignal : IntegrationIds(ReservedSubscriberAttribute.ONESIGNAL_ID)
        public object OneSignalUserId : IntegrationIds(ReservedSubscriberAttribute.ONESIGNAL_USER_ID)
        public object Airship : IntegrationIds(ReservedSubscriberAttribute.AIRSHIP_CHANNEL_ID)
        public object FirebaseAppInstanceId : IntegrationIds(ReservedSubscriberAttribute.FIREBASE_APP_INSTANCE_ID)
        public object TenjinAnalyticsInstallationId : IntegrationIds(
            ReservedSubscriberAttribute.TENJIN_ANALYTICS_INSTALLATION_ID,
        )
        public object PostHogUserId : IntegrationIds(ReservedSubscriberAttribute.POSTHOG_USER_ID)
    }

    public sealed class CampaignParameters(
        backendKey: ReservedSubscriberAttribute,
    ) : SubscriberAttributeKey(backendKey.value) {
        public object MediaSource : CampaignParameters(ReservedSubscriberAttribute.MEDIA_SOURCE)
        public object Campaign : CampaignParameters(ReservedSubscriberAttribute.CAMPAIGN)
        public object AdGroup : CampaignParameters(ReservedSubscriberAttribute.AD_GROUP)
        public object Ad : CampaignParameters(ReservedSubscriberAttribute.AD)
        public object Keyword : CampaignParameters(ReservedSubscriberAttribute.KEYWORD)
        public object Creative : CampaignParameters(ReservedSubscriberAttribute.CREATIVE)
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
