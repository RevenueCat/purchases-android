@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.paywallstester.ui.screens.paywall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesHolder
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: PaywallScreenViewModel = viewModel<PaywallScreenViewModelImpl>(),
    dismissRequest: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val isRefreshing = viewModel.isRefreshing.collectAsStateWithLifecycle().value
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshOffering() },
        modifier = Modifier.fillMaxSize(),
    ) {
        when (state) {
            is PaywallScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is PaywallScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Error: ${state.errorMessage}")
                }
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
            is PaywallScreenState.WorkflowLoaded -> {
                Paywall(
                    PaywallOptions.Builder(dismissRequest)
                        .setWorkflowIdentifier(state.workflowId)
                        .setListener(viewModel)
                        .build(),
                )
                state.dialogText?.let {
                    PurchaseAlertDialog(viewModel, it)
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
