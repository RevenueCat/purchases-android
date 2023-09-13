package com.revenuecat.paywallstester.ui.screens.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ui.revenuecatui.PaywallView
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions

@Composable
fun PaywallScreen(
    viewModel: PaywallScreenViewModel = viewModel<PaywallScreenViewModelImpl>(),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = viewModel.state.collectAsState().value) {
            is PaywallScreenState.Loading -> {
                Text(text = "Loading...")
            }
            is PaywallScreenState.Error -> {
                Text(text = "Error: ${state.errorMessage}")
            }
            is PaywallScreenState.Loaded -> {
                PaywallView(
                    PaywallViewOptions.Builder()
                        .setOffering(state.offering)
                        .setListener(viewModel)
                        .build(),
                )
                if (state.displayCompletedPurchaseMessage) {
                    PurchaseAlertDialog(viewModel, "Purchase was successful")
                } else if (state.displayErrorPurchasingMessage) {
                    PurchaseAlertDialog(viewModel, "There was a purchasing error")
                }
            }
        }
    }
}

@Composable
private fun PurchaseAlertDialog(
    viewModel: PaywallScreenViewModel,
    text: String,
) {
    AlertDialog(
        onDismissRequest = {
            viewModel.onDismissed()
        },
        buttons = {
            TextButton(
                onClick = {
                    viewModel.onDismissed()
                },
            ) {
                Text("Ok")
            }
        },
        text = {
            Text(text)
        },
    )
}
