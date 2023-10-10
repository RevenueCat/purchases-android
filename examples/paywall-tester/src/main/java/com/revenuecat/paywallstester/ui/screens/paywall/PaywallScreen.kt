package com.revenuecat.paywallstester.ui.screens.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.ui.theme.superMarioFontFamily
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType

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
                Paywall(
                    PaywallOptions.Builder()
                        .setOffering(state.offering)
                        .setFontProvider(object : FontProvider {
                            override fun getFont(type: TypographyType): FontFamily? {
                                return superMarioFontFamily
                            }
                        })
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
