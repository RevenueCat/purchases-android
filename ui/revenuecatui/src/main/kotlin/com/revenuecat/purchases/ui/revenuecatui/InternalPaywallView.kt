package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2

@Composable
internal fun InternalPaywallView(
    offering: Offering? = null,
    listener: PaywallViewListener? = null,
    viewModel: PaywallViewModel = getPaywallViewModel(offering = offering, listener = listener),
) {
    updateStateIfLocaleChanged(viewModel)

    when (val state = viewModel.state.collectAsState().value) {
        is PaywallViewState.Loading -> {
            Text(text = "Loading...")
        }
        is PaywallViewState.Error -> {
            Text(text = "Error: ${state.errorMessage}")
        }
        is PaywallViewState.Loaded -> {
            when (state.templateConfiguration.template) {
                PaywallTemplate.TEMPLATE_1 -> Text(text = "Error: Template 1 not supported")
                PaywallTemplate.TEMPLATE_2 -> Template2(state = state, viewModel = viewModel)
                PaywallTemplate.TEMPLATE_3 -> Text(text = "Error: Template 3 not supported")
                PaywallTemplate.TEMPLATE_4 -> Text(text = "Error: Template 4 not supported")
                PaywallTemplate.TEMPLATE_5 -> Text(text = "Error: Template 5 not supported")
            }
        }
    }
}

@Composable
private fun updateStateIfLocaleChanged(viewModel: PaywallViewModel) {
    var locale by remember { mutableStateOf(LocaleListCompat.getDefault()) }
    if (locale != LocaleListCompat.getDefault()) {
        locale = LocaleListCompat.getDefault()
        viewModel.refreshState()
    }
}

@Composable
private fun getPaywallViewModel(offering: Offering?, listener: PaywallViewListener?): PaywallViewModel {
    return viewModel<PaywallViewModelImpl>(
        factory = PaywallViewModelFactory(LocalContext.current.applicationContext, offering, listener),
    )
}
