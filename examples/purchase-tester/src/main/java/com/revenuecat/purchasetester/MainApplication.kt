package com.revenuecat.purchasetester

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.RedeemRCBillingPurchaseListener
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainApplication : Application(), UpdatedCustomerInfoListener, RedeemRCBillingPurchaseListener {

    val logHandler = TesterLogHandler(this)

    // This should live in a repository, but for the sake of simplicity we'll keep it here
    val lastCustomerInfoLiveData: LiveData<CustomerInfo?>
        get() = lastCustomerInfoMutableLiveData
    private var lastCustomerInfoMutableLiveData = MutableLiveData<CustomerInfo?>(null)

    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.DEBUG

        Purchases.logHandler = logHandler
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        lastCustomerInfoMutableLiveData.postValue(customerInfo)
        val message = "CustomerInfoListener received update at ${customerInfo.requestDate}"
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT,
        ).show()
        Log.d("CustomerInfoListener", "$message: $customerInfo")
    }

    override fun handleRCBillingPurchaseRedemption(
        redemptionStarter: RedeemRCBillingPurchaseListener.RedemptionStarter,
    ) {
        redemptionStarter.startRedemption(object : RedeemRCBillingPurchaseListener.ResultListener {
            override fun handleResult(result: RedeemRCBillingPurchaseListener.RedeemResult) {
                val message = when (result) {
                    RedeemRCBillingPurchaseListener.RedeemResult.SUCCESS -> "Redeem RC Billing purchase successful"
                    RedeemRCBillingPurchaseListener.RedeemResult.ERROR -> "Redeem RC Billing purchase failed"
                }
                Toast.makeText(
                    this@MainApplication,
                    message,
                    Toast.LENGTH_SHORT,
                ).show()
                Log.d("Purchase Tester", message)
            }
        })
    }
}

fun showError(message: String) {
    Log.e("Purchase Tester", message)
}

fun showError(error: PurchasesError) {
    showError(error.message)
}

fun showUserError(activity: Activity, error: PurchasesError) {
    MaterialAlertDialogBuilder(activity)
        .setMessage(error.message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
}
