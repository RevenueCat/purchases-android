package com.revenuecat.purchases.galaxy

import android.content.Context
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener
import com.samsung.android.sdk.iap.lib.listener.OnChangeSubscriptionPlanListener
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener
import com.samsung.android.sdk.iap.lib.listener.OnGetOwnedListListener
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.listener.OnGetPromotionEligibilityListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener
import com.samsung.android.sdk.iap.lib.util.HelperUtil

internal class DefaultIAPHelperProvider(
    val iapHelper: IapHelper,
) : IAPHelperProvider {

    override fun setOperationMode(
        mode: HelperDefine.OperationMode,
    ) {
        log(LogIntent.DEBUG) { GalaxyStrings.SETTING_OPERATION_MODE.format(mode.description()) }
        iapHelper.setOperationMode(mode)
    }

    override fun isAcknowledgeAvailable(context: Context): Boolean {
        return HelperUtil.isAcknowledgeAvailable(context)
    }

    @GalaxySerialOperation
    override fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener,
    ) {
        iapHelper.getProductsDetails(
            productIDs,
            onGetProductsDetailsListener,
        )
    }

    @GalaxySerialOperation
    override fun getPromotionEligibility(
        itemIDs: String,
        onGetPromotionEligibilityListener: OnGetPromotionEligibilityListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnGetPromotionEligibilityListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.getPromotionEligibility(
            itemIDs,
            onGetPromotionEligibilityListener,
        )
    }

    @GalaxySerialOperation
    override fun startPayment(
        itemId: String,
        obfuscatedAccountId: String?,
        obfuscatedProfileId: String?,
        onPaymentListener: OnPaymentListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnPaymentListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.startPayment(
            itemId,
            obfuscatedAccountId,
            obfuscatedProfileId,
            onPaymentListener,
        )
    }

    @GalaxySerialOperation
    override fun acknowledgePurchases(
        purchaseIds: String,
        onAcknowledgePurchasesListener: OnAcknowledgePurchasesListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnAcknowledgePurchasesListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.acknowledgePurchases(
            purchaseIds,
            onAcknowledgePurchasesListener,
        )
    }

    @GalaxySerialOperation
    override fun consumePurchaseItems(
        purchaseIds: String,
        onConsumePurchasedItemsListener: OnConsumePurchasedItemsListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnConsumePurchasedItemsListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.consumePurchasedItems(
            purchaseIds,
            onConsumePurchasedItemsListener,
        )
    }

    @GalaxySerialOperation
    override fun getOwnedList(
        onGetOwnedListListener: OnGetOwnedListListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnGetOwnedListListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.getOwnedList(
            HelperDefine.PRODUCT_TYPE_ALL,
            onGetOwnedListListener,
        )
    }

    @GalaxySerialOperation
    override fun changeSubscriptionPlan(
        oldItemId: String,
        newItemId: String,
        prorationMode: HelperDefine.ProrationMode,
        obfuscatedAccountId: String?,
        obfuscatedProfileId: String?,
        onChangeSubscriptionPlanListener: OnChangeSubscriptionPlanListener,
    ): Boolean {
        // Return values:
        // true: The request was sent to server successfully and the result will be sent
        //       to OnChangeSubscriptionPlanListener interface listener.
        // false: The request was not sent to server and was not processed.
        return iapHelper.changeSubscriptionPlan(
            oldItemId,
            newItemId,
            prorationMode,
            obfuscatedAccountId,
            obfuscatedProfileId,
            onChangeSubscriptionPlanListener,
        )
    }
}
