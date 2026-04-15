@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpener
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod

/**
 * Internal composable that fetches a workflow by identifier and renders its initial step
 * using the existing component paywall pipeline.
 *
 * This is not public API. It is intended for use by other SDK modules and tester apps
 * via [InternalRevenueCatAPI] opt-in.
 */
@InternalRevenueCatAPI
@Suppress("ModifierMissing")
@Composable
public fun WorkflowView(
    identifier: String,
    dismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = getWorkflowViewModel(identifier = identifier, context = context)
    val state = viewModel.state.collectAsStateWithLifecycle().value

    when (state) {
        is WorkflowState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is WorkflowState.Error -> {
            ErrorDialog(
                dismissRequest = dismissRequest,
                error = state.message,
            )
        }
        is WorkflowState.Loaded -> {
            LoadedPaywallComponents(
                state = state.paywallState,
                clickHandler = rememberWorkflowActionHandler(viewModel, dismissRequest),
            )
            viewModel.actionError.value?.let { error ->
                ErrorDialog(
                    dismissRequest = viewModel::clearActionError,
                    error = error.message,
                )
            }
        }
    }
}

@Stable
@Composable
private fun getWorkflowViewModel(
    identifier: String,
    context: Context,
): WorkflowViewModelImpl {
    return viewModel<WorkflowViewModelImpl>(
        key = identifier,
        factory = WorkflowViewModelFactory(
            workflowId = identifier,
            purchases = PurchasesImpl(),
            resourceProvider = context.applicationContext.toResourceProvider(),
        ),
    )
}

@Composable
private fun rememberWorkflowActionHandler(
    viewModel: WorkflowViewModel,
    dismissRequest: () -> Unit,
): suspend (PaywallAction.External) -> Unit {
    val context: Context = LocalContext.current
    val activity: Activity? = context.getActivity()
    return remember(viewModel, dismissRequest) {
        {
                action ->
            when (action) {
                is PaywallAction.External.NavigateBack -> dismissRequest()

                is PaywallAction.External.RestorePurchases -> viewModel.handleRestorePurchases()

                is PaywallAction.External.PurchasePackage -> {
                    if (activity == null) {
                        Logger.e("WorkflowView: activity is null, cannot initiate purchase")
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
                        Logger.e("WorkflowView: web checkout URL cannot be found")
                    } else {
                        viewModel.invalidateCustomerInfoCache()
                        context.openUrl(url, action.openMethod)
                        if (action.autoDismiss) dismissRequest()
                    }
                }

                is PaywallAction.External.NavigateTo -> when (val destination = action.destination) {
                    is PaywallAction.External.NavigateTo.Destination.CustomerCenter ->
                        Logger.w("WorkflowView: Customer Center is not yet implemented on Android.")
                    is PaywallAction.External.NavigateTo.Destination.Url ->
                        context.openUrl(destination.url, destination.method)
                }
            }
        }
    }
}

private fun Context.openUrl(url: String, method: ButtonComponent.UrlMethod) {
    val openingMethod = when (method) {
        ButtonComponent.UrlMethod.IN_APP_BROWSER -> URLOpeningMethod.IN_APP_BROWSER
        ButtonComponent.UrlMethod.EXTERNAL_BROWSER -> URLOpeningMethod.EXTERNAL_BROWSER
        ButtonComponent.UrlMethod.DEEP_LINK -> URLOpeningMethod.DEEP_LINK
        ButtonComponent.UrlMethod.UNKNOWN -> {
            Logger.e("WorkflowView: ignoring button click with unknown open method for URL: '$url'")
            return
        }
    }
    URLOpener.openURL(this, url, openingMethod)
}
