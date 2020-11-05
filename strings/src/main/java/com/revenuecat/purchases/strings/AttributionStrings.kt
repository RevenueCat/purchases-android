package com.revenuecat.purchases.strings

object AttributionStrings {
    const val ATTRIBUTE_ERROR = Emojis.SAD_CAT_EYES + Emojis.ERROR + " There were some subscriber attributes errors: %s"
    const val ATTRIBUTES_SYNC_SUCCESS = Emojis.HEART_CAT_EYES + " Subscriber attributes synced successfully for " +
            "App User ID: %s"
    const val COLLECT_DEVICE_IDS = Emojis.INFO + " collectDeviceIdentifiers called"
    const val DELETE_ATTRIBUTES = Emojis.INFO + " Deleting subscriber attributes for %s from cache"
    const val DELETE_ATTRIBUTES_OTHER_USERS = Emojis.INFO + " Deleting old synced subscriber attributes that don't " +
            "belong to %s"
    const val INVALID_SUBSCRIBER_ATTRIBUTES = Emojis.SAD_CAT_EYES + Emojis.ERROR + " One or more of the attributes " +
            "sent could not be saved."
    const val MARK_ATTRIBUTES_SYNC_SUCCESS = Emojis.INFO + " Marking the following attributes as synced for " +
            "App User ID: %s"
    const val NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE = Emojis.INFO + " No subscriber attributes to synchronize."
    const val SAME_ATTRIBUTES = Emojis.INFO + " Attribution data is the same as latest. Skipping."
    const val SET_AD = Emojis.INFO + " setAd called"
    const val SET_ADGROUP = Emojis.INFO + " setAdGroup called"
    const val SET_ADJUST_ID = Emojis.INFO + " setAdjustID called"
    const val SET_APPSFLYER_ID = Emojis.INFO + " setAppsflyerId called"
    const val SET_ATTRIBUTES = Emojis.INFO + " setAttributes called"
    const val SET_CAMPAIGN = Emojis.INFO + " setCampaign called"
    const val SET_CREATIVE = Emojis.INFO + " setCreative called"
    const val SET_DISPLAY_NAME = Emojis.INFO + " setDisplayName called"
    const val SET_EMAIL = Emojis.INFO + " setEmail called"
    const val SET_FB_ANON_ID = Emojis.INFO + " setFBAnonymousID called"
    const val SET_KEYWORD = Emojis.INFO + " setKeyword called"
    const val SET_MEDIA_SOURCE = Emojis.INFO + " setMediaSource called"
    const val SET_MPARTICLE_ID = Emojis.INFO + " setMparticleID called"
    const val SET_ONESIGNAL_ID = Emojis.INFO + " setOnesignalID called"
    const val SET_PHONE_NUMBER = Emojis.INFO + " setPhoneNumber called"
    const val SET_PUSH_TOKEN = Emojis.INFO + " setPushToken called"
    const val SYNCING_ATTRIBUTES_ERROR = Emojis.SAD_CAT_EYES + Emojis.ERROR + " Error when syncing subscriber " +
            "attributes. App User ID: %s, Error: %s"
    const val UNSYNCED_ATTRIBUTES_COUNT = Emojis.INFO + " Found %d unsynced attributes for App User ID: %s"
}
