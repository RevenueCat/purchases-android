package com.revenuecat.sample.paywall

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Suppress("LongParameterList")
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    purchaseListener: PurchaseListener? = null,
    marketingContent: (@Composable () -> Unit)? = null,
) {
    val viewModel: PaywallViewModel = viewModel()
    Scaffold { padding ->
        val state = viewModel.uiState.collectAsState()
        when (val currentState = state.value) {
            PaywallState.Loading -> {
                PaywallLoadingView(
                    modifier = modifier.padding(padding),
                )
            }
            is PaywallState.Error -> {
                PaywallErrorView(
                    errorMessage = currentState.message,
                    modifier = modifier.padding(padding),
                )
            }
            is PaywallState.Success -> {
                PaywallSuccessView(
                    uiState = currentState,
                    modifier = modifier.padding(padding),
                    purchaseListener = purchaseListener,
                    marketingContent = marketingContent,
                )
            }
        }
    }
}
