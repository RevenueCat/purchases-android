// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.CustomerCenterLoadingView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Renders an offering's inline/default paywall using the same simulated Customer Center session.
 * Purchase and restore use [provider]; SDK analytics are disabled. URL and web checkout actions are intercepted.
 * Hosts should use this for [CustomerCenterPreviewAction.ShowPaywall] and dismiss it before the parent preview.
 */
@InternalRevenueCatAPI
@Composable
public fun CustomerCenterPreviewPaywall(
    provider: CustomerCenterPreviewProvider,
    offering: Offering,
    isDarkMode: Boolean? = null,
    onDismiss: () -> Unit,
) {
    val latestDismiss = rememberUpdatedState(onDismiss)
    val dark = isDarkMode ?: isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    val resources = LocalContext.current.applicationContext.toResourceProvider()
    val workflowState by rememberPreviewWorkflow(provider, offering)
    val workflow = when (val state = workflowState) {
        PreviewWorkflowState.Loading -> {
            CustomerCenterLoadingView()
            return
        }
        is PreviewWorkflowState.Error -> {
            PreviewWorkflowError(state.error) { latestDismiss.value() }
            return
        }
        is PreviewWorkflowState.Loaded -> state.workflow
    }
    val options = remember(provider, offering, workflow) {
        previewPaywallOptions(offering, workflow) { latestDismiss.value() }
    }
    val store = remember(provider, offering, workflow) { ViewModelStore() }
    val model = remember(store) {
        ViewModelProvider(
            store,
            viewModelFactory {
                initializer {
                    PaywallViewModelImpl(
                        resourceProvider = resources,
                        purchases = CustomerCenterPreviewPurchases(provider),
                        options = options,
                        colorScheme = colors,
                        isDarkMode = dark,
                        shouldDisplayBlock = null,
                        preview = true,
                    )
                }
            },
        )[PaywallViewModelImpl::class.java]
    }
    DisposableEffect(store) { onDispose { store.clear() } }
    val scope = rememberCoroutineScope()
    val uriHandler = remember(provider) {
        object : UriHandler {
            override fun openUri(uri: String) {
                scope.launch { provider.handleAction(CustomerCenterPreviewAction.OpenUrl(uri)) }
            }
        }
    }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        InternalPaywall(
            options = options,
            viewModel = model,
            isDarkModeOverride = dark,
            externalActionInterceptor = { action ->
                interceptPreviewPaywallAction(action, provider, model)
            },
        )
    }
}

@Composable
private fun PreviewWorkflowError(error: PurchasesError, onDismiss: () -> Unit) {
    ErrorDialog(
        dismissRequest = onDismiss,
        error = error.underlyingErrorMessage?.takeIf(String::isNotBlank)
            ?: stringResource(R.string.revenuecatui_preview_workflow_load_error),
    )
}

/** Uses the production URL resolver while keeping checkout and URL navigation inside the preview. */
internal suspend fun interceptPreviewPaywallAction(
    action: PaywallAction.External,
    provider: CustomerCenterPreviewProvider,
    viewModel: PaywallViewModel,
): Boolean = when (action) {
    is PaywallAction.External.NavigateTo -> {
        val destination = action.destination as? PaywallAction.External.NavigateTo.Destination.Url
        if (destination != null) {
            provider.handleAction(CustomerCenterPreviewAction.OpenUrl(destination.url))
            true
        } else {
            false
        }
    }
    is PaywallAction.External.LaunchWebCheckout -> {
        val url = viewModel.getWebCheckoutUrl(action)
        if (url != null) {
            provider.handleAction(CustomerCenterPreviewAction.OpenUrl(url))
            if (action.autoDismiss) viewModel.closePaywall()
        }
        true
    }
    else -> false
}

internal fun previewPaywallOptions(
    offering: Offering,
    workflow: CustomerCenterPreviewWorkflow?,
    onDismiss: () -> Unit,
): PaywallOptions = PaywallOptions.Builder(onDismiss).setOffering(offering)
    .setShouldDisplayDismissButton(true).apply {
        workflow?.let {
            injectedWorkflow(
                it.workflow,
                it.offerings,
                it.uiConfig,
                traceId = null,
                workflowBlobRef = null,
            )
        }
    }.build()

internal sealed interface PreviewWorkflowState {
    object Loading : PreviewWorkflowState
    data class Loaded(val workflow: CustomerCenterPreviewWorkflow?) : PreviewWorkflowState
    data class Error(val error: PurchasesError) : PreviewWorkflowState
}

@Composable
private fun rememberPreviewWorkflow(
    provider: CustomerCenterPreviewProvider,
    offering: Offering,
): State<PreviewWorkflowState> {
    val state = remember(provider, offering) { mutableStateOf<PreviewWorkflowState>(PreviewWorkflowState.Loading) }
    LaunchedEffect(provider, offering) {
        state.value = loadPreviewWorkflow(provider, offering)
    }
    return state
}

/** Provider failures remain inside the preview; dismissal still cancels in-flight work. */
@Suppress("TooGenericExceptionCaught")
internal suspend fun loadPreviewWorkflow(
    provider: CustomerCenterPreviewProvider,
    offering: Offering,
): PreviewWorkflowState = try {
    PreviewWorkflowState.Loaded(provider.workflow(offering))
} catch (e: CancellationException) {
    throw e
} catch (e: PurchasesException) {
    PreviewWorkflowState.Error(e.error)
} catch (_: Exception) {
    PreviewWorkflowState.Error(PurchasesError(PurchasesErrorCode.ConfigurationError))
}
