package com.revenuecat.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.purchasePackageWith
import kotlinx.android.synthetic.main.activity_upsell.annual_purchase
import kotlinx.android.synthetic.main.activity_upsell.monthly_purchase
import kotlinx.android.synthetic.main.activity_upsell.skip
import kotlinx.android.synthetic.main.activity_upsell.unlimited_purchase

class UpsellActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upsell)

        showScreen()
        skip.setOnClickListener { startCatsActivity() }
        Purchases.sharedInstance.updatedPurchaserInfoListener =
            UpdatedPurchaserInfoListener(::checkForProEntitlement)
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getOfferingsWith(::showError, ::showScreen)
    }

    private fun showScreen(
        offerings: Offerings? = null
    ) {
        if (offerings == null) {
            monthly_purchase.text = "Loading..."
            monthly_purchase.isEnabled = false
            annual_purchase.text = "Loading..."
            annual_purchase.isEnabled = false
        } else {
            offerings.current?.let { currentOffering ->
                setupPackageButton(currentOffering.monthly, monthly_purchase)
                setupPackageButton(currentOffering.annual, annual_purchase)
                setupPackageButton(currentOffering.lifetime, unlimited_purchase)
            } ?: showError("Error loading current offering")
        }
    }

     private fun setupPackageButton(aPackage: Package?, button: Button) {
        aPackage?.product?.let { product ->
            with(button) {
                loadedText =
                    "Buy ${aPackage.packageType} - ${product.priceCurrencyCode} ${product.price}"
                showLoading(false)
                setOnClickListener {
                    makePurchase(aPackage, this)
                }
            }
        } ?: showError("Error finding package")
    }

    private fun makePurchase(
        packageToPurchase: Package,
        button: Button
    ) {
        button.showLoading(true)
        Purchases.sharedInstance.purchasePackageWith(
            this,
            packageToPurchase,
            { error, userCancelled ->
                if (!userCancelled) {
                    showError(error)
                }
            }) { _, purchaserInfo ->
            button.showLoading(false)
            checkForProEntitlement(purchaserInfo)
        }
    }

    private fun checkForProEntitlement(purchaserInfo: PurchaserInfo) {
        if (purchaserInfo.entitlements[PREMIUM_ENTITLEMENT_ID]?.isActive == true) {
            startCatsActivity()
        }
    }

    private fun Button.showLoading(loading: Boolean) {
        text = if (loading) "Loading..." else loadedText
        isEnabled = !loading
    }
}

private var Button.loadedText: String
    get() {
        return (tag as String)
    }
    set(value) {
        tag = value
    }
