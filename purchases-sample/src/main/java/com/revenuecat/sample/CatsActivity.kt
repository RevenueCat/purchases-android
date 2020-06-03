package com.revenuecat.sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.android.synthetic.main.activity_cats.*

class CatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cats)

        go_premium.setOnClickListener {
            finish()
        }
        purchase_restore.setOnClickListener {
            purchase_restore.text = "Loading..."
            Purchases.sharedInstance.restorePurchasesWith(::showError) { purchaserInfo ->
                purchase_restore.text = "Restore Purchases"
                configureContent(purchaserInfo)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getPurchaserInfoWith(::showError, ::configureContent)
    }

    private fun configureContent(purchaserInfo: PurchaserInfo) {
        if (purchaserInfo.entitlements[PREMIUM_ENTITLEMENT_ID]?.isActive == true) {
            Log.i("Purchases Sample", "Hey there premium, you're a happy cat 😻")
            cat_content_label.text = "😻"
            go_premium.visibility = GONE
            purchase_restore.visibility = GONE
            val dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext)

            purchaserInfo.getPurchaseDateForEntitlement(PREMIUM_ENTITLEMENT_ID)?.let {
                purchase_date_label.text = "Purchase Date: ${dateFormat.format(it)}"
                purchase_date_label.visibility = VISIBLE
            }

            purchaserInfo.getExpirationDateForEntitlement(PREMIUM_ENTITLEMENT_ID)?.let {
                expiration_date_label.text = "Expiration Date: ${dateFormat.format(it)}"
                expiration_date_label.visibility = VISIBLE
            }
        } else {
            Log.i("Purchases Sample", "Happy cats are only for premium members \uD83D\uDE3F")
            cat_content_label.text = "😿"
            go_premium.visibility = VISIBLE
            purchase_restore.visibility = VISIBLE
            expiration_date_label.visibility = GONE
            purchase_date_label.visibility = GONE
        }

        if (purchaserInfo.managementURL != null) {
            manage_subscription.visibility = VISIBLE
            manage_subscription.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(purchaserInfo.managementURL)))
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        } else {
            manage_subscription.visibility = GONE
        }
    }
}
