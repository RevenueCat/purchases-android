package com.revenuecat.paywallstester.ui.screens.paywallfooter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreenState
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreenViewModel
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreenViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.OriginalTemplatePaywallFooter
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@Composable
fun PaywallFooterScreen(
    viewModel: PaywallScreenViewModel = viewModel<PaywallScreenViewModelImpl>(),
    dismissRequest: () -> Unit,
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
                OriginalTemplatePaywallFooter(
                    options = PaywallOptions.Builder(dismissRequest)
                        .setOffering(state.offering)
                        .setListener(viewModel)
                        .build(),
                    condensed = state.footerCondensed,
                ) {
                    SamplePaywall(paddingValues = it)
                }
                state.dialogText?.let {
                    PurchaseAlertDialog(viewModel, it)
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
internal fun SamplePaywall(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues),
    ) {
        // TODO-Paywalls: Implement an actual sample paywall
        for (i in 1..50) {
            Text(text = "Main content $i")
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
            viewModel.onDialogDismissed()
        },
        buttons = {
            TextButton(
                onClick = {
                    viewModel.onDialogDismissed()
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
