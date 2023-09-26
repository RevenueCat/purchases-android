package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.helpers.toAndroidContext
import com.revenuecat.purchases.ui.revenuecatui.templates.Template1
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2

@Composable
internal fun InternalPaywallView(
    mode: PaywallViewMode = PaywallViewMode.default,
    offering: Offering? = null,
    listener: PaywallViewListener? = null,
    viewModel: PaywallViewModel = getPaywallViewModel(offering = offering, listener = listener, mode = mode),
) {
    viewModel.refreshStateIfLocaleChanged()
    val colors = MaterialTheme.colorScheme
    viewModel.refreshStateIfColorsChanged(colors)

    when (val state = viewModel.state.collectAsState().value) {
        is PaywallViewState.Loading -> {
            Text(text = "Loading...")
        }
        is PaywallViewState.Error -> {
            Text(text = "Error: ${state.errorMessage}")
        }
        is PaywallViewState.Loaded -> {
            when (state.templateConfiguration.template) {
                PaywallTemplate.TEMPLATE_1 -> Template1(state = state, viewModel = viewModel)
                PaywallTemplate.TEMPLATE_2 -> Template2(state = state, viewModel = viewModel)
                PaywallTemplate.TEMPLATE_3 -> Text(text = "Error: Template 3 not supported")
                PaywallTemplate.TEMPLATE_4 -> Text(text = "Error: Template 4 not supported")
                PaywallTemplate.TEMPLATE_5 -> Text(text = "Error: Template 5 not supported")
            }
        }
    }
}

@Composable
private fun getPaywallViewModel(
    mode: PaywallViewMode,
    offering: Offering?,
    listener: PaywallViewListener?,
): PaywallViewModel {
    val applicationContext = LocalContext.current.applicationContext
    return viewModel<PaywallViewModelImpl>(
        factory = PaywallViewModelFactory(
            applicationContext.toAndroidContext(),
            mode,
            offering,
            listener,
            MaterialTheme.colorScheme,
        ),
    )
}
