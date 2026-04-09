package com.revenuecat.paywallstester.ui.screens.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesHolder
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@Composable
fun PaywallScreen(
    viewModel: PaywallScreenViewModel = viewModel<PaywallScreenViewModelImpl>(),
    dismissRequest: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
                is PaywallScreenState.Loading -> {
                    Text(text = "Loading...")
                }
                is PaywallScreenState.Error -> {
                    Text(text = "Error: ${state.errorMessage}")
                }
                is PaywallScreenState.Loaded -> {
                    // Use key to force full recomposition (and new PaywallViewModelImpl)
                    // when refreshCount changes. The internal Paywall ViewModel is keyed
                    // by PaywallOptions.hashCode(), which includes customVariables,
                    // so injecting a refresh token ensures a fresh ViewModel on each refresh.
                    val customVariables = CustomVariablesHolder.customVariables +
                        mapOf("refresh_token" to CustomVariableValue.String("${state.refreshCount}"))
                    key(state.refreshCount) {
                        Paywall(
                            PaywallOptions.Builder(dismissRequest)
                                .setOffering(state.offering)
                                .setListener(viewModel)
                                .setCustomVariables(customVariables)
                                .build(),
                        )
                    }
                    state.dialogText?.let {
                        PurchaseAlertDialog(viewModel, it)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.refreshOffering() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh paywall",
            )
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
