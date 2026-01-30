package com.revenuecat.purchases

import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import java.util.Date
import kotlin.time.Duration

internal class SyncPurchasesHelper(
    private val billing: BillingAbstract,
    private val identityManager: IdentityManager,
    private val customerInfoHelper: CustomerInfoHelper,
    private val postReceiptHelper: PostReceiptHelper,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    fun syncPurchases(
        isRestore: Boolean,
        appInBackground: Boolean,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG) { PurchaseStrings.SYNCING_PURCHASES }

        val startTime = dateProvider.now
        diagnosticsTrackerIfEnabled?.trackSyncPurchasesStarted()

        val appUserID = identityManager.currentAppUserID

        val handleSuccess: (CustomerInfo) -> Unit = {
            trackSyncPurchasesResultIfNeeded(null, startTime)
            onSuccess(it)
        }

        val handleError: (PurchasesError) -> Unit = {
            trackSyncPurchasesResultIfNeeded(it, startTime)
            onError(it)
        }

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                if (allPurchases.isNotEmpty()) {
                    val lastPurchase = allPurchases.last()
                    val errors: MutableList<PurchasesError> = mutableListOf()
                    fun handleLastPurchase(currentPurchase: StoreTransaction, lastPurchase: StoreTransaction) {
                        if (currentPurchase == lastPurchase) {
                            if (errors.isEmpty()) {
                                debugLog { PurchaseStrings.SYNCED_PURCHASES_SUCCESSFULLY }
                                retrieveCustomerInfo(appUserID, appInBackground, isRestore, handleSuccess, handleError)
                            } else {
                                errorLog { PurchaseStrings.SYNCING_PURCHASES_ERROR.format(errors) }
                                handleError(errors.first())
                            }
                        }
                    }
                    allPurchases.forEach { purchase ->
                        val productInfo = ReceiptInfo(
                            productIDs = purchase.productIds,
                            purchaseTime = purchase.purchaseTime,
                            storeUserID = purchase.storeUserID,
                            marketplace = purchase.marketplace,
                        )
                        postReceiptHelper.postTokenWithoutConsuming(
                            purchase.purchaseToken,
                            productInfo,
                            isRestore,
                            appUserID,
                            PostReceiptInitiationSource.RESTORE,
                            {
                                log(LogIntent.PURCHASE) { PurchaseStrings.PURCHASE_SYNCED.format(purchase) }
                                handleLastPurchase(purchase, lastPurchase)
                            },
                            { error ->
                                log(LogIntent.RC_ERROR) {
                                    PurchaseStrings.SYNCING_PURCHASES_ERROR_DETAILS
                                        .format(purchase, error)
                                }
                                errors.add(error)
                                handleLastPurchase(purchase, lastPurchase)
                            },
                        )
                    }
                } else {
                    retrieveCustomerInfo(appUserID, appInBackground, isRestore, handleSuccess, handleError)
                }
            },
            onReceivePurchaseHistoryError = {
                log(LogIntent.RC_ERROR) { PurchaseStrings.SYNCING_PURCHASES_ERROR.format(it) }
                handleError(it)
            },
        )
    }

    private fun retrieveCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        isRestore: Boolean,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        customerInfoHelper.retrieveCustomerInfo(
            appUserID,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            appInBackground = appInBackground,
            allowSharingPlayStoreAccount = isRestore,
            callback = object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    onSuccess(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    onError(error)
                }
            },
        )
    }

    private fun trackSyncPurchasesResultIfNeeded(
        error: PurchasesError?,
        startTime: Date,
    ) {
        diagnosticsTrackerIfEnabled?.trackSyncPurchasesResult(
            error?.code?.code,
            error?.message,
            Duration.between(startTime, dateProvider.now),
        )
    }
}
