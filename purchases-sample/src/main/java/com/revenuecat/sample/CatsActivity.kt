package com.revenuecat.sample

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import kotlinx.android.synthetic.main.activity_cats.cat_content_label
import kotlinx.android.synthetic.main.activity_cats.expiration_date_label
import kotlinx.android.synthetic.main.activity_cats.go_premium
import kotlinx.android.synthetic.main.activity_cats.purchase_date_label
import kotlinx.android.synthetic.main.activity_cats.purchase_restore

class CatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cats)

        go_premium.setOnClickListener {
            finish()
        }
        purchase_restore.setOnClickListener {
            purchase_restore.text = "Loading..."
            Purchases.sharedInstance.restorePurchases({ purchaserInfo ->
                purchase_restore.text = "Restore Purchases"
                configureContentFor(purchaserInfo!!)
            }, { error ->
                showError(error.message!!)
            })
        }

        Purchases.sharedInstance.getPurchaserInfo({ purchaserInfo ->
            configureContentFor(purchaserInfo!!)
        }, { error ->
            showError(error.message!!)
        })
    }

    private fun configureContentFor(purchaserInfo: PurchaserInfo) {
        if (purchaserInfo.activeEntitlements.contains("pro")) {
            Log.i("Purchases Sample", "Hey there premium, you're a happy cat 😻")
            cat_content_label.text = "😻"
            go_premium.visibility = GONE
            purchase_restore.visibility = GONE
            val dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext)

            purchaserInfo.getPurchaseDateForEntitlement("pro")?.let {
                purchase_date_label.text = "Purchase Date: ${dateFormat.format(it)}"
                purchase_date_label.visibility = VISIBLE
            }

            purchaserInfo.getExpirationDateForEntitlement("pro")?.let {
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
    }
}
