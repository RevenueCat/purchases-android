package com.revenuecat.purchases.ui.revenuecatui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ui.revenuecatui.UIConstant.defaultAnimation
import com.revenuecat.purchases.ui.revenuecatui.composables.CloseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallTheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.toAndroidContext
import com.revenuecat.purchases.ui.revenuecatui.templates.Template1
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2
import com.revenuecat.purchases.ui.revenuecatui.templates.Template3
import com.revenuecat.purchases.ui.revenuecatui.templates.Template4
import com.revenuecat.purchases.ui.revenuecatui.templates.Template5

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
internal fun InternalPaywall(
    options: PaywallOptions,
    viewModel: PaywallViewModel = getPaywallViewModel(options),
) {
    BackHandler {
        viewModel.closePaywall()
    }
    PaywallTheme(fontProvider = options.fontProvider) {
        viewModel.refreshStateIfLocaleChanged()
        viewModel.refreshStateIfColorsChanged(MaterialTheme.colorScheme, isSystemInDarkTheme())

        val state = viewModel.state.collectAsState().value

        AnimatedVisibility(
            visible = state is PaywallState.Loading || state is PaywallState.Error,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            LoadingPaywall(
                mode = options.mode,
                shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                onDismiss = viewModel::closePaywall,
            )
        }

        AnimatedVisibility(
            visible = state is PaywallState.Loaded,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            LoadedPaywall(state = state as PaywallState.Loaded, viewModel = viewModel)
        }

        when (state) {
            is PaywallState.Loading -> {}

            is PaywallState.Error -> {
                ErrorDialog(
                    dismissRequest = options.dismissRequest,
                    error = state.errorMessage,
                )
            }

            is PaywallState.Loaded -> {
                viewModel.actionError.value?.let {
                    ErrorDialog(
                        dismissRequest = viewModel::clearActionError,
                        error = it.message,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedPaywall(state: PaywallState.Loaded, viewModel: PaywallViewModel) {
    viewModel.trackPaywallImpressionIfNeeded()
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
                            topStart = UIConstant.defaultCornerRadius,
                            topEnd = UIConstant.defaultCornerRadius,
                        ),
                    )
                    .background(backgroundColor)
            },
    ) {
        TemplatePaywall(state = state, viewModel = viewModel)
        CloseButton(state.shouldDisplayDismissButton, viewModel::closePaywall)
    }
}

@Composable
private fun TemplatePaywall(state: PaywallState.Loaded, viewModel: PaywallViewModel) {
    when (state.templateConfiguration.template) {
        PaywallTemplate.TEMPLATE_1 -> Template1(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_2 -> Template2(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_3 -> Template3(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_4 -> Template4(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_5 -> Template5(state = state, viewModel = viewModel)
    }
}

@ExperimentalPreviewRevenueCatUIPurchasesAPI
@Composable
internal fun getPaywallViewModel(
    options: PaywallOptions,
): PaywallViewModel {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel = viewModel<PaywallViewModelImpl>(
        factory = PaywallViewModelFactory(
            applicationContext.toAndroidContext(),
            options,
            MaterialTheme.colorScheme,
            isSystemInDarkTheme(),
            preview = isInPreviewMode(),
        ),
    )
    viewModel.updateOptions(options)
    return viewModel
}

@Composable
private fun ErrorDialog(
    dismissRequest: () -> Unit,
    error: String,
) {
    AlertDialog(
        onDismissRequest = dismissRequest,
        confirmButton = {
            TextButton(
                onClick = dismissRequest,
            ) {
                Text(
                    text = stringResource(id = R.string.OK),
                    textAlign = TextAlign.Center,
                )
            }
        },
        icon = { Icon(painter = painterResource(id = R.drawable.error), contentDescription = null) },
        text = {
            Text(text = error)
        },
    )
}
