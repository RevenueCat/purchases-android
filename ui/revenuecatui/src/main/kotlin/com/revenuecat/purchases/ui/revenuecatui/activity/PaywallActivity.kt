package com.revenuecat.purchases.ui.revenuecatui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.GoogleFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFont
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import com.revenuecat.purchases.ui.revenuecatui.getPaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.restoreSdkConfigurationIfNeeded
import com.revenuecat.purchases.ui.revenuecatui.helpers.saveSdkConfiguration

/**
 * Wrapper activity around [Paywall] that allows using it when you are not using Jetpack Compose directly.
 * It receives the [PaywallActivityArgs] as an extra and returns the [PaywallResult] as a result.
 */
@Suppress("TooManyFunctions")
internal class PaywallActivity : ComponentActivity(), PaywallListener {
    companion object {
        const val ARGS_EXTRA = "paywall_args"
        const val RESULT_EXTRA = "paywall_result"
    }

    private val exitOfferLauncher: ActivityResultLauncher<PaywallActivityArgs> =
        registerForActivityResult(PaywallContract()) { result ->
            setResult(RESULT_OK, createResultIntent(result))
            finish()
        }

    private fun getArgs(): PaywallActivityArgs? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ARGS_EXTRA, PaywallActivityArgs::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(ARGS_EXTRA)
        }
    }

    private fun getFontProvider(): FontProvider? {
        val googleFontProviders = mutableMapOf<GoogleFontProvider, GoogleFont.Provider>()
        val fontsMap = getArgs()?.fonts?.mapValues { (_, fontFamily) ->
            val fonts = fontFamily?.fonts?.map { font ->
                when (font) {
                    is PaywallFont.ResourceFont -> Font(font.resourceId, font.fontWeight, FontStyle(font.fontStyle))
                    is PaywallFont.AssetFont -> Font(font.path, assets, font.fontWeight, FontStyle(font.fontStyle))
                    is PaywallFont.GoogleFont -> {
                        val googleFontProvider = font.fontProvider
                        val provider = googleFontProviders.getOrElse(googleFontProvider) {
                            val googleProvider = googleFontProvider.toGoogleProvider()
                            googleFontProviders[googleFontProvider] = googleProvider
                            googleProvider
                        }
                        Font(GoogleFont(font.fontName), provider, font.fontWeight, FontStyle(font.fontStyle))
                    }
                }
            }
            fonts?.let { FontFamily(it) }
        } ?: return null
        return object : FontProvider {
            override fun getFont(type: TypographyType): FontFamily? {
                return fontsMap[type]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        restoreSdkConfigurationIfNeeded(this, savedInstanceState)

        val args = getArgs()
        val wasLaunchedThroughSDK = args?.wasLaunchedThroughSDK ?: false
        if (!wasLaunchedThroughSDK && !Purchases.isConfigured) {
            Logger.e(
                "PaywallActivity was launched incorrectly. " +
                    "Please use PaywallActivityLauncher, or Paywall/PaywallDialog/PaywallFooter " +
                    "composables to display the Paywall.",
            )
            finish()
            return
        }

        val edgeToEdge = args?.edgeToEdge == true
        if (edgeToEdge) {
            enableEdgeToEdge()
        }

        val offeringSelection = args?.offeringIdAndPresentedOfferingContext

        setContent {
            MaterialTheme {
                Scaffold { paddingValues ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .conditional(!edgeToEdge) {
                                padding(paddingValues)
                            },
                    ) {
                        // Empty dismissRequest is overridden below by setDismissRequestWithExitOffering
                        val paywallOptions = PaywallOptions.Builder(
                            dismissRequest = {},
                        )
                            .setOfferingSelection(offeringSelection)
                            .setFontProvider(getFontProvider())
                            .setShouldDisplayDismissButton(
                                args?.shouldDisplayDismissButton ?: DEFAULT_DISPLAY_DISMISS_BUTTON,
                            )
                            .setListener(this@PaywallActivity)
                            .setDismissRequestWithExitOffering(::onDismissRequest)
                            .build()
                        val viewModel = getPaywallViewModel(paywallOptions)

                        LaunchedEffect(Unit) {
                            viewModel.preloadExitOffering()
                        }

                        Paywall(paywallOptions)
                    }
                }
            }
        }
    }

    private fun onDismissRequest(exitOffering: Offering?) {
        if (exitOffering != null) {
            launchExitOfferActivity(exitOffering)
        } else {
            finish()
        }
    }

    private fun launchExitOfferActivity(exitOffering: Offering) {
        val currentArgs = getArgs() ?: run {
            finish()
            return
        }
        // Launch the exit offer activity on top of this one
        // When it finishes, exitOfferLauncher callback will forward its result and finish this activity
        val exitOfferArgs = currentArgs.copy(
            offeringIdAndPresentedOfferingContext = OfferingSelection.IdAndPresentedOfferingContext(
                offeringId = exitOffering.identifier,
                presentedOfferingContext = null,
            ),
        )
        exitOfferLauncher.launch(exitOfferArgs)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveSdkConfiguration(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
        setResult(RESULT_OK, createResultIntent(PaywallResult.Purchased(customerInfo)))
        finish()
    }

    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
        setResult(RESULT_OK, createResultIntent(PaywallResult.Restored(customerInfo)))
        val requiredEntitlementIdentifier = getArgs()?.requiredEntitlementIdentifier ?: return
        if (customerInfo.entitlements.active.containsKey(requiredEntitlementIdentifier)) {
            finish()
        }
    }

    override fun onPurchaseError(error: PurchasesError) {
        val result = if (error.code == PurchasesErrorCode.PurchaseCancelledError) {
            PaywallResult.Cancelled
        } else {
            PaywallResult.Error(error)
        }
        setResult(RESULT_OK, createResultIntent(result))
    }

    override fun onRestoreError(error: PurchasesError) {
        setResult(RESULT_OK, createResultIntent(PaywallResult.Error(error)))
    }

    private fun createResultIntent(result: PaywallResult): Intent {
        return Intent().putExtra(RESULT_EXTRA, result)
    }
}
