package com.revenuecat.sample.paywall

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = viewModel(),
    modifier: Modifier = Modifier,
    marketingContent: (@Composable () -> Unit)? = null,
) {
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
                PaywallView(
                    offering = currentState.offering,
                    modifier = modifier.padding(padding),
                    marketingContent = marketingContent,
                )
            }
        }
    }
}
