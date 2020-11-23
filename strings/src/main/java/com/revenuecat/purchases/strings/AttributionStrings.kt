package com.revenuecat.purchases.strings

object AttributionStrings {
    const val ATTRIBUTE_ERROR = "There were some subscriber attributes errors: %s"
    const val ATTRIBUTES_SYNC_SUCCESS = "Subscriber attributes synced successfully for App User ID: %s"
    const val DELETE_ATTRIBUTES = "Deleting subscriber attributes for %s from cache"
    const val DELETE_ATTRIBUTES_OTHER_USERS = "Deleting old synced subscriber attributes that don't belong to %s"
    const val MARK_ATTRIBUTES_SYNC_SUCCESS = "Marking the following attributes as synced for App User ID: %s"
    const val NO_SUBSCRIBER_ATTRIBUTES_TO_SYNCHRONIZE = "No subscriber attributes to synchronize."
    const val SAME_ATTRIBUTES = "Attribution data is the same as latest. Skipping."
    const val SET_STRING = "%s called"
    const val SYNCING_ATTRIBUTES_ERROR = "Error when syncing subscriber attributes. App User ID: %s, Error: %s"
    const val UNSYNCED_ATTRIBUTES_COUNT = "Found %d unsynced attributes for App User ID: %s"
}
