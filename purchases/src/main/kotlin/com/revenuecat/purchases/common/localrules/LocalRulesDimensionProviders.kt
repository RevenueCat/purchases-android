package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsReceiptStore
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute

@Suppress("LongParameterList")
internal fun localRulesDimensionProviders(
    appConfig: AppConfig,
    localeProvider: LocaleProvider,
    storefront: suspend () -> String?,
    attributes: () -> Map<String, SubscriberAttribute>,
    customerInfo: suspend (appUserId: String) -> CustomerInfo,
    dimensions: suspend () -> SubscriberDimensionsResolution,
    receiptStore: SubscriberDimensionsReceiptStore,
    currentAppUserId: () -> String,
): List<RulesDimensionProvider> = listOf(
    DeviceDimensionProvider(appConfig, localeProvider),
    StoreDimensionProvider(storefront),
    SubscriberAttributesDimensionProvider(attributes),
    CustomerInfoDimensionProvider(currentAppUserId, customerInfo),
    SubscriberDimensionsProvider(dimensions, receiptStore, currentAppUserId),
)
