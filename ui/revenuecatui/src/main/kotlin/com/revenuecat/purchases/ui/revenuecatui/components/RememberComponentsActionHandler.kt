package com.revenuecat.purchases.ui.revenuecatui.components

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpener
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod

/**
 * Creates a remembered action handler that dispatches [PaywallAction.External] events
 * to the given [viewModel].
 *
 * @param viewModel the ViewModel that handles purchases, restores, and web checkout.
 * @param onDismiss called when the UI should be dismissed (NavigateBack, auto-dismiss after web checkout).
 */
@Composable
internal fun rememberComponentsActionHandler(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
): suspend (PaywallAction.External) -> Unit {
    val context: Context = LocalContext.current
    val activity: Activity? = context.getActivity()
    return remember(viewModel, onDismiss) {
        {
                action ->
            when (action) {
                is PaywallAction.External.NavigateBack -> onDismiss()

                is PaywallAction.External.RestorePurchases -> viewModel.handleRestorePurchases()

                is PaywallAction.External.PurchasePackage -> {
                    if (activity == null) {
                        Logger.e("Activity is null, not initiating package purchase")
                    } else {
                        viewModel.handlePackagePurchase(
                            activity = activity,
                            pkg = action.rcPackage,
                            resolvedOffer = action.resolvedOffer,
                        )
                    }
                }

                is PaywallAction.External.LaunchWebCheckout -> {
                    val url = viewModel.getWebCheckoutUrl(action)
                    if (url == null) {
                        Logger.e("Web checkout URL cannot be found, not launching web checkout.")
                    } else {
                        viewModel.invalidateCustomerInfoCache()
                        context.openUrl(url, action.openMethod)
                        if (action.autoDismiss) {
                            Logger.d("Auto-dismissing after launching web checkout.")
                            onDismiss()
                        }
                    }
                }

                is PaywallAction.External.NavigateTo -> when (val destination = action.destination) {
                    is PaywallAction.External.NavigateTo.Destination.CustomerCenter ->
                        Logger.w("Customer Center is not yet implemented on Android.")
                    is PaywallAction.External.NavigateTo.Destination.Url ->
                        context.openUrl(destination.url, destination.method)
                }
            }
        }
    }
}

internal fun Context.openUrl(url: String, method: ButtonComponent.UrlMethod) {
    val openingMethod = when (method) {
        ButtonComponent.UrlMethod.IN_APP_BROWSER -> URLOpeningMethod.IN_APP_BROWSER
        ButtonComponent.UrlMethod.EXTERNAL_BROWSER -> URLOpeningMethod.EXTERNAL_BROWSER
        ButtonComponent.UrlMethod.DEEP_LINK -> URLOpeningMethod.DEEP_LINK
        ButtonComponent.UrlMethod.UNKNOWN -> {
            Logger.e("Ignoring button click with unknown open method for URL: '$url'.")
            return
        }
    }
    URLOpener.openURL(this, url, openingMethod)
}
