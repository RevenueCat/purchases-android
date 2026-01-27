package com.revenuecat.paywallstester.ui.screens.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicWithCallback

@Composable
fun PaywallScreen(
    viewModel: PaywallScreenViewModel = viewModel<PaywallScreenViewModelImpl>(),
    dismissRequest: () -> Unit,
) {
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
                Paywall(
                    PaywallOptions.Builder(dismissRequest)
                        .setOffering(state.offering)
                        .setListener(viewModel)
                        .setPurchaseLogic(object: PurchaseLogicWithCallback() {
                            override fun performPurchaseWithCompletion(
                                activity: Activity,
                                rcPackage: Package,
                                completion: (PurchaseLogicResult) -> Unit,
                            ) {
                                viewModel.performMyAppLogicPurchase(activity, rcPackage, completion)
                            }

                            override fun performRestoreWithCompletion(
                                customerInfo: CustomerInfo,
                                completion: (PurchaseLogicResult) -> Unit,
                            ) {
                                // TODO("Not yet implemented")
                            }
                        })
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
