package com.revenuecat.sample.paywall

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError

@Suppress("LongParameterList")
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    onPurchaseStarted: ((Package) -> Unit)? = null,
    onPurchaseCompleted: ((CustomerInfo) -> Unit)? = null,
    onPurchaseCancelled: (() -> Unit)? = null,
    onPurchaseErrored: ((PurchasesError) -> Unit)? = null,
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
                PaywallView(
                    offering = currentState.offering,
                    modifier = modifier.padding(padding),
                    onPurchaseStarted = onPurchaseStarted,
                    onPurchaseCompleted = onPurchaseCompleted,
                    onPurchaseCancelled = onPurchaseCancelled,
                    onPurchaseErrored = onPurchaseErrored,
                    marketingContent = marketingContent,
                )
            }
        }
    }
}
