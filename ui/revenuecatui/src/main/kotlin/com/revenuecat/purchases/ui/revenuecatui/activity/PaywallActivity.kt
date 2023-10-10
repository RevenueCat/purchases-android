package com.revenuecat.purchases.ui.revenuecatui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType

/**
 * Wrapper activity around [Paywall] that allows using it when you are not using Jetpack Compose directly.
 * It receives the [PaywallActivityArgs] as an extra and returns the [PaywallResult] as a result.
 */
internal class PaywallActivity : ComponentActivity(), PaywallListener {
    companion object {
        const val ARGS_EXTRA = "paywall_args"

        const val RESULT_EXTRA = "paywall_result"
    }

    private fun getArgs(): PaywallActivityArgs? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ARGS_EXTRA, PaywallActivityArgs::class.java)
        } else {
            intent.getParcelableExtra(ARGS_EXTRA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = getArgs()
        val fontProvider = args?.mapFonts?.let { mapFonts ->
            object : FontProvider {
                override fun getFont(type: TypographyType): FontFamily? {
                    return mapFonts[type]?.let { fontRes ->
                        return ResourcesCompat.getFont(this@PaywallActivity, fontRes)?.let { FontFamily(it) }
                    }
                }
            }
        }
        val paywallOptions = PaywallOptions.Builder()
            .setOfferingId(getArgs()?.offeringId)
            .setFontProvider(fontProvider)
            .setListener(this)
            .build()
        setContent {
            MaterialTheme {
                Scaffold { paddingValues ->
                    Box(Modifier.fillMaxSize().padding(paddingValues)) {
                        Paywall(paywallOptions)
                    }
                }
            }
        }
    }

    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
        setResult(RESULT_OK, createResultIntent(PaywallResult.Purchased(customerInfo)))
        finish()
    }

    override fun onPurchaseError(error: PurchasesError) {
        val result = if (error.code == PurchasesErrorCode.PurchaseCancelledError) {
            PaywallResult.Cancelled
        } else {
            PaywallResult.Error(error)
        }
        setResult(RESULT_OK, createResultIntent(result))
    }

    private fun createResultIntent(result: PaywallResult): Intent {
        return Intent().putExtra(RESULT_EXTRA, result)
    }
}
