package com.revenuecat.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Entitlement
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getEntitlementsWith
import com.revenuecat.purchases.makePurchaseWith
import kotlinx.android.synthetic.main.activity_upsell.*

class UpsellActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upsell)

        showScreen()
        skip.setOnClickListener { startCatsActivity() }
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getEntitlementsWith(::showError) { entitlementMap ->
            showScreen(entitlementMap)
        }
    }

    private fun showScreen(
        entitlementMap: Map<String, Entitlement>? = null
    ) {
        if (entitlementMap == null) {
            monthly_purchase.text = "Loading..."
            monthly_purchase.isEnabled = false
            annual_purchase.text = "Loading..."
            annual_purchase.isEnabled = false
        } else {
            entitlementMap["pro"]?.let { proEntitlement ->
                setupMonthlyOfferingButton(proEntitlement)
                setupAnnualOfferingButton(proEntitlement)
            } ?: showError("Error finding pro entitlement")
        }
    }

    private fun setupMonthlyOfferingButton(proEntitlement: Entitlement) {
        proEntitlement.offerings["monthly"]?.let { monthly ->
            monthly.skuDetails?.let { monthlyProduct ->
                with(monthly_purchase) {
                    loadedText = "Buy Monthly - " +
                            "${monthlyProduct.priceCurrencyCode}" +
                            "${monthlyProduct.price}"
                    showLoading(false)
                    setOnClickListener {
                        makePurchase(monthlyProduct, this)
                    }
                }
            } ?: showError("Error finding monthly active product")
        } ?: showError("Error finding monthly offering")
    }

    private fun setupAnnualOfferingButton(proEntitlement: Entitlement) {
        proEntitlement.offerings["annual"]?.let { annual ->
            annual.skuDetails?.let { annualProduct ->
                with(annual_purchase) {
                    loadedText = "Buy Monthly - " +
                            "${annualProduct.priceCurrencyCode}" +
                            "${annualProduct.price}"
                    showLoading(false)
                    setOnClickListener {
                        makePurchase(annualProduct, this)
                    }
                }
            } ?: showError("Error finding annual active product")
        } ?: showError("Error finding annual offering")
    }

    private fun makePurchase(product: SkuDetails, button: Button) {
        button.showLoading(true)
        Purchases.sharedInstance.makePurchaseWith(
            this,
            product.sku,
            BillingClient.SkuType.SUBS,
            ::showError) { _, purchaserInfo ->
                button.showLoading(false)
                if (purchaserInfo.activeEntitlements.contains("pro")) {
                    startCatsActivity()
                }
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
