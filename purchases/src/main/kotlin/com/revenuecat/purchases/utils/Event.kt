package com.revenuecat.purchases.utils

/**
 * Interface for events that can be tracked through an [EventsFileHelper]
 */
internal interface Event {
    override fun toString(): String
}
