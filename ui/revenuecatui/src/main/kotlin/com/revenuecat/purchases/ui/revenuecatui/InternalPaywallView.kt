package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.toAndroidContext
import com.revenuecat.purchases.ui.revenuecatui.templates.Template1
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2
import com.revenuecat.purchases.ui.revenuecatui.templates.Template3

@Composable
internal fun InternalPaywallView(
    options: PaywallViewOptions,
    viewModel: PaywallViewModel = getPaywallViewModel(options),
) {
    viewModel.refreshStateIfLocaleChanged()
    val colors = MaterialTheme.colorScheme
    viewModel.refreshStateIfColorsChanged(colors)

    when (val state = viewModel.state.collectAsState().value) {
        is PaywallViewState.Loading -> {
            LoadingPaywallView(mode = options.mode)
        }
        is PaywallViewState.Error -> {
            Text(text = "Error: ${state.errorMessage}")
        }
        is PaywallViewState.Loaded -> {
            val backgroundColor = state.templateConfiguration.getCurrentColors().background
            Box(
                modifier = Modifier
                    .conditional(state.isInFullScreenMode) {
                        Modifier.background(backgroundColor)
                    }
                    .conditional(!state.isInFullScreenMode) {
                        Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = footerRoundedBorderHeight,
                                    topEnd = footerRoundedBorderHeight,
                                ),
                            )
                            .background(backgroundColor)
                            .padding(top = footerRoundedBorderHeight)
                    },
            ) {
                when (state.templateConfiguration.template) {
                    PaywallTemplate.TEMPLATE_1 -> Template1(state = state, viewModel = viewModel)
                    PaywallTemplate.TEMPLATE_2 -> Template2(state = state, viewModel = viewModel)
                    PaywallTemplate.TEMPLATE_3 -> Template3(state = state, viewModel = viewModel)
                    PaywallTemplate.TEMPLATE_4 -> Text(text = "Error: Template 4 not supported")
                    PaywallTemplate.TEMPLATE_5 -> Text(text = "Error: Template 5 not supported")
                }
            }
        }
    }
}

@Composable
private fun getPaywallViewModel(
    options: PaywallViewOptions,
): PaywallViewModel {
    val applicationContext = LocalContext.current.applicationContext
    return viewModel<PaywallViewModelImpl>(
        // We need to pass the key in order to create different view models for different offerings when
        // trying to load different paywalls for the same view model store owner.
        key = options.offering?.identifier,
        factory = PaywallViewModelFactory(
            applicationContext.toAndroidContext(),
            options,
            MaterialTheme.colorScheme,
            preview = isInPreviewMode(),
        ),
    )
}
