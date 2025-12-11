package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener

internal class DefaultIAPHelperProvider(
    val iapHelper: IapHelper,
) : IAPHelperProvider {

    override fun setOperationMode(
        mode: HelperDefine.OperationMode
    ) {
        log(LogIntent.DEBUG) { GalaxyStrings.SETTING_OPERATION_MODE.format(mode.description()) }
        iapHelper.setOperationMode(mode)
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
}
