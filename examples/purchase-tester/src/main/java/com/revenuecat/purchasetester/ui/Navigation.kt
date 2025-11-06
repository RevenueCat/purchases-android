package com.revenuecat.purchasetester.ui

import kotlinx.serialization.Serializable

@Serializable
object ConfigureRoute

@Serializable
object LoginRoute

@Serializable
object OverviewRoute

@Serializable
data class OfferingRoute(val offeringId: String)

@Serializable
object LogsRoute

@Serializable
object ProxySettingsRoute
