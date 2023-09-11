package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2

@Composable
internal fun InternalPaywallView(
    offering: Offering? = null,
    viewModel: PaywallViewModel = getPaywallViewModel(offering = offering),
) {
    when (val state = viewModel.state.collectAsState().value) {
        is PaywallViewState.Loading -> {
            Text(text = "Loading...")
        }
        is PaywallViewState.Error -> {
            Text(text = "Error: ${state.errorMessage}")
        }
        is PaywallViewState.Template2 -> {
            Template2(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun getPaywallViewModel(offering: Offering?): PaywallViewModel {
    return viewModel<PaywallViewModelImpl>(
        factory = PaywallViewModelFactory(offering),
    )
}
