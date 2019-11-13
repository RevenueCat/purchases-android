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
import kotlinx.android.synthetic.main.activity_upsell.*

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
                setupMonthlyButton(currentOffering.monthly)
                setupAnnualButton(currentOffering.annual)
                setupUnlimitedButton(currentOffering.lifetime)
            } ?: showError("Error loading current offering")
        }
    }

    private fun setupUnlimitedButton(lifetimePackage: Package?) {
        lifetimePackage?.product?.let { unlimitedProduct ->
            with(unlimited_purchase) {
                loadedText =
                    "Buy Unlimited - ${unlimitedProduct.priceCurrencyCode} ${unlimitedProduct.price}"
                showLoading(false)
                setOnClickListener {
                    makePurchase(lifetimePackage, this)
                }
            }
        } ?: showError("Error loading lifetime package")
    }

    private fun setupMonthlyButton(monthlyPackage: Package?) {
        monthlyPackage?.product?.let { monthlyProduct ->
            with(monthly_purchase) {
                loadedText =
                    "Buy Monthly - ${monthlyProduct.priceCurrencyCode} ${monthlyProduct.price}"
                showLoading(false)
                setOnClickListener {
                    makePurchase(monthlyPackage, this)
                }
            }
        } ?: showError("Error loading monthly package")
    }

    private fun setupAnnualButton(annualPackage: Package?) {
        annualPackage?.product?.let { annualProduct ->
            with(annual_purchase) {
                loadedText =
                    "Buy Annual - ${annualProduct.priceCurrencyCode} ${annualProduct.price}"
                showLoading(false)
                setOnClickListener {
                    makePurchase(annualPackage, this)
                }
            }
        } ?: showError("Error finding annual package")
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
        if (purchaserInfo.entitlements["pro_cat"]?.isActive == true) {
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
