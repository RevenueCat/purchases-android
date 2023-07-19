package com.revenuecat.purchases

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager

class PurchasesOrchestrator internal constructor(
    private val application: Application,
    backingFieldAppUserID: String?,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val deviceCache: DeviceCache,
    private val identityManager: IdentityManager,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    @set:JvmSynthetic @get:JvmSynthetic internal var appConfig: AppConfig,
    private val customerInfoHelper: CustomerInfoHelper,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    diagnosticsSynchronizer: DiagnosticsSynchronizer?,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val postReceiptHelper: PostReceiptHelper,
    private val postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper,
    private val postPendingTransactionsHelper: PostPendingTransactionsHelper,
    private val syncPurchasesHelper: SyncPurchasesHelper,
    private val offeringsManager: OfferingsManager,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) {
}