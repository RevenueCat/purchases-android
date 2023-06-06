package com.revenuecat.purchases.strings

object AttributionStrings {
    const val ATTRIBUTES_SYNC_ERROR = "Error when syncing subscriber attributes. App User ID: %s, Error: %s"
    const val ATTRIBUTES_SYNC_SUCCESS = "Subscriber attributes synced successfully for App User ID: %s"
    const val DELETING_ATTRIBUTES = "Deleting subscriber attributes for %s from cache"
    const val DELETING_ATTRIBUTES_OTHER_USERS = "Deleting old synced subscriber attributes that don't belong to %s"
    const val COPYING_ATTRIBUTES_FROM_TO_USER = "Copying unsynced subscriber attributes from user %s to user %s"
    const val GOOGLE_PLAY_SERVICES_NOT_INSTALLED_FETCHING_ADVERTISING_IDENTIFIER = "GooglePlayServices is not " +
        "installed. Couldn't get advertising identifier. Message: %s"
    const val GOOGLE_PLAY_SERVICES_REPAIRABLE_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER =
        "GooglePlayServicesRepairableException when getting advertising identifier. Message: %s"
    const val IO_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER = "IOException when getting advertising " +
        "identifier. Message: %s"
    const val MARKING_ATTRIBUTES_SYNCED = "Marking the following attributes as synced for App User ID: %s"
    const val METHOD_CALLED = "%s called"
    const val NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE = "No subscriber attributes to synchronize."
    const val SUBSCRIBER_ATTRIBUTES_ERROR = "There were some subscriber attributes errors: %s"
    const val TIMEOUT_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER = "TimeoutException when getting advertising " +
        "identifier. Message: %s"
    const val UNSYNCED_ATTRIBUTES_COUNT = "Found %d unsynced attributes for App User ID: %s"
    const val AMAZON_COULD_NOT_GET_ADID = "Couldn't get Amazon advertising identifier. Message: %s"
}
