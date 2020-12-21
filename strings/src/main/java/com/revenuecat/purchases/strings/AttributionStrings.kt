package com.revenuecat.purchases.strings

object AttributionStrings {
    const val ATTRIBUTES_SYNC_ERROR = "Error when syncing subscriber attributes. App User ID: %s, Error: %s"
    const val ATTRIBUTES_SYNC_SUCCESS = "Subscriber attributes synced successfully for App User ID: %s"
    const val DELETING_ATTRIBUTES = "Deleting subscriber attributes for %s from cache"
    const val DELETING_ATTRIBUTES_OTHER_USERS = "Deleting old synced subscriber attributes that don't belong to %s"
    const val MARKING_ATTRIBUTES_SYNCED = "Marking the following attributes as synced for App User ID: %s"
    const val METHOD_CALLED = "%s called"
    const val NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE = "No subscriber attributes to synchronize."
    const val SKIP_SAME_ATTRIBUTES = "Attribution data is the same as latest. Skipping."
    const val SUBSCRIBER_ATTRIBUTES_ERROR = "There were some subscriber attributes errors: %s"
    const val UNSYNCED_ATTRIBUTES_COUNT = "Found %d unsynced attributes for App User ID: %s"
}
