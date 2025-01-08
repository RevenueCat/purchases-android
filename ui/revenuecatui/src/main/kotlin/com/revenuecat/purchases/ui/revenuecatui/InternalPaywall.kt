package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.UIConstant.defaultAnimation
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.composables.CloseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.openUriOrElse
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallTheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.templates.Template1
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2
import com.revenuecat.purchases.ui.revenuecatui.templates.Template3
import com.revenuecat.purchases.ui.revenuecatui.templates.Template4
import com.revenuecat.purchases.ui.revenuecatui.templates.Template5
import com.revenuecat.purchases.ui.revenuecatui.templates.Template7

@Suppress("LongMethod")
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
            visible = state is PaywallState.Loaded.Legacy,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            if (state is PaywallState.Loaded.Legacy) {
                LoadedPaywall(state = state, viewModel = viewModel)
            } else {
                Logger.e(
                    "State is not loaded while transitioning animation. This may happen if state changes from " +
                        "being loaded to a different state. This should not happen.",
                )
            }
        }

        AnimatedVisibility(
            visible = state is PaywallState.Loaded.Components,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            if (state is PaywallState.Loaded.Components) {
                LoadedPaywallComponents(
                    state = state,
                    clickHandler = rememberPaywallActionHandler(viewModel),
                )
            } else {
                Logger.e(
                    "State is not Loaded.Components while transitioning animation. This may happen if state changes " +
                        "from being loaded to a different state. This should not happen.",
                )
            }
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
private fun LoadedPaywall(state: PaywallState.Loaded.Legacy, viewModel: PaywallViewModel) {
    viewModel.trackPaywallImpressionIfNeeded()
    val backgroundColor = state.templateConfiguration.getCurrentColors().background
    Box(
        modifier = Modifier
            .conditional(state.isInFullScreenMode) {
                Modifier
                    .fillMaxHeight()
                    .background(backgroundColor)
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
        val configuration = state.configurationWithOverriddenLocale()

        CompositionLocalProvider(
            LocalActivity provides LocalContext.current.getActivity(),
            LocalContext provides state.contextWithConfiguration(configuration),
            LocalConfiguration provides configuration,
        ) {
            TemplatePaywall(state = state, viewModel = viewModel)
        }
        CloseButton(
            shouldDisplayDismissButton = state.shouldDisplayDismissButton,
            color = state.currentColors.closeButton,
            actionInProgress = viewModel.actionInProgress.value,
            onClick = viewModel::closePaywall,
        )
    }
}

@Composable
private fun TemplatePaywall(state: PaywallState.Loaded.Legacy, viewModel: PaywallViewModel) {
    when (state.templateConfiguration.template) {
        PaywallTemplate.TEMPLATE_1 -> Template1(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_2 -> Template2(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_3 -> Template3(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_4 -> Template4(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_5 -> Template5(state = state, viewModel = viewModel)
        PaywallTemplate.TEMPLATE_7 -> Template7(state = state, viewModel = viewModel)
    }
}

@Composable
internal fun getPaywallViewModel(
    options: PaywallOptions,
    shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null,
): PaywallViewModel {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel = viewModel<PaywallViewModelImpl>(
        key = options.dataHash,
        factory = PaywallViewModelFactory(
            applicationContext.toResourceProvider(),
            options,
            MaterialTheme.colorScheme,
            isSystemInDarkTheme(),
            shouldDisplayBlock = shouldDisplayBlock,
            preview = isInPreviewMode(),
        ),
    )
    viewModel.updateOptions(options)
    return viewModel
}

@ReadOnlyComposable
@Composable
private fun PaywallState.Loaded.Legacy.contextWithConfiguration(configuration: Configuration): Context {
    val context = LocalContext.current

    // Context.createConfigurationContext returns `null` with Paparazzi
    return context.createConfigurationContext(configuration) ?: context
}

@ReadOnlyComposable
@Composable
private fun PaywallState.Loaded.Legacy.configurationWithOverriddenLocale(): Configuration {
    val configuration = Configuration(LocalConfiguration.current)
    configuration.setLocale(templateConfiguration.locale)

    return configuration
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

@Composable
private fun rememberPaywallActionHandler(viewModel: PaywallViewModel): suspend (PaywallAction) -> Unit {
    val context: Context = LocalContext.current
    val activity: Activity? = context.getActivity()
    return remember(viewModel) {
        {
                action ->
            when (action) {
                is PaywallAction.RestorePurchases -> viewModel.handleRestorePurchases()
                is PaywallAction.PurchasePackage ->
                    if (activity == null) {
                        Logger.e("Activity is null, not initiating package purchase")
                    } else {
                        viewModel.handlePackagePurchase(activity)
                    }

                is PaywallAction.NavigateBack -> viewModel.closePaywall()
                is PaywallAction.NavigateTo -> when (val destination = action.destination) {
                    is PaywallAction.NavigateTo.Destination.CustomerCenter ->
                        Logger.w("Customer Center is not yet implemented on Android.")
                    is PaywallAction.NavigateTo.Destination.Url -> context.handleUrlDestination(
                        url = destination.url,
                        method = destination.method,
                    )
                }
            }
        }
    }
}

private fun Context.handleUrlDestination(url: String, method: ButtonComponent.UrlMethod) {
    fun handleException(exception: Exception) {
        val message = if (exception is ActivityNotFoundException) {
            getString(R.string.no_browser_cannot_open_link)
        } else {
            getString(R.string.cannot_open_link)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Logger.e(message, exception)
    }

    when (method) {
        ButtonComponent.UrlMethod.IN_APP_BROWSER -> {
            val intent = CustomTabsIntent.Builder()
                .build()
            @Suppress("TooGenericExceptionCaught")
            try {
                intent.launchUrl(this, Uri.parse(url))
            } catch (e: Exception) {
                handleException(e)
            }
        }

        ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
        ButtonComponent.UrlMethod.DEEP_LINK,
        -> openUriOrElse(url, ::handleException)
    }
}

/**
 * Returns the activity from a given context. Most times, the context itself will be
 * an activity, but in the case it's not, it will iterate through the context wrappers until it
 * finds one that is an activity.
 */
private fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
