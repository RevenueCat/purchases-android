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
import kotlinx.android.synthetic.main.activity_upsell.annual_purchase
import kotlinx.android.synthetic.main.activity_upsell.monthly_purchase
import kotlinx.android.synthetic.main.activity_upsell.skip

class UpsellActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upsell)

        showScreen(true)
        Purchases.sharedInstance.getEntitlementsWith(::showError) { entitlementMap ->
            showScreen(false, entitlementMap)
        }
        skip.setOnClickListener { startCats() }
    }

    private fun showScreen(
        loading: Boolean,
        entitlementMap: Map<String, Entitlement>? = null
    ) {
        if (loading) {
            monthly_purchase.text = "Loading..."
            monthly_purchase.isEnabled = false
            annual_purchase.text = "Loading..."
            annual_purchase.isEnabled = false
        } else {
            entitlementMap?.let { entitlements ->
                entitlements["pro"]?.let { proEntitlement ->
                    loadMonthlyOffering(proEntitlement)
                    loadAnnualOffering(proEntitlement)
                } ?: showError("Error finding pro entitlement")
            }
        }
    }

    private fun loadMonthlyOffering(proEntitlement: Entitlement) {
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

    private fun loadAnnualOffering(proEntitlement: Entitlement) {
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
                    startCats()
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
