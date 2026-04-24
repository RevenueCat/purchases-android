@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
import com.revenuecat.purchases.ui.revenuecatui.UIConstant.defaultAnimation
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.composables.CloseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.defaultpaywall.DefaultPaywallView
import com.revenuecat.purchases.ui.revenuecatui.extensions.ProvideLayoutDirection
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.resolveLayoutDirection
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallTheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallLegacyComponentInteraction
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallProductIdentifier
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPurchaseButtonAction
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.templates.Template1
import com.revenuecat.purchases.ui.revenuecatui.templates.Template2
import com.revenuecat.purchases.ui.revenuecatui.templates.Template3
import com.revenuecat.purchases.ui.revenuecatui.templates.Template4
import com.revenuecat.purchases.ui.revenuecatui.templates.Template5
import com.revenuecat.purchases.ui.revenuecatui.templates.Template7
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpener
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod

@Suppress("LongMethod", "ViewModelForwarding")
@Composable
internal fun InternalPaywall(
    options: PaywallOptions,
    viewModel: PaywallViewModel = getPaywallViewModel(options),
) {
    BackHandler {
        viewModel.closePaywall()
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    SideEffect {
        viewModel.refreshStateIfLocaleChanged()
        viewModel.refreshStateIfColorsChanged(colorScheme = colorScheme, isDark = isDark)
    }

    val state = viewModel.state.collectAsStateWithLifecycle().value
    val localeList = viewModel.localeList.collectAsStateWithLifecycle().value
    val honorLayoutDirection = viewModel.preferredUILocaleOverrideHonorsLayoutDirection.collectAsStateWithLifecycle().value

    val componentInteractionTracker = remember(viewModel) {
        PaywallComponentInteractionTracker { data ->
            viewModel.trackComponentInteraction(data)
        }
    }

    PaywallTheme(fontProvider = options.fontProvider) {
        AnimatedVisibility(
            visible = state is PaywallState.Loading || state is PaywallState.Error,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            LoadingPaywall(
                mode = options.mode,
                shouldDisplayDismissButton = options.shouldDisplayDismissButton,
                localeList = localeList,
                honorLayoutDirection = honorLayoutDirection,
                onDismiss = viewModel::closePaywall,
            )
        }
    }

    PaywallTheme(fontProvider = options.fontProvider) {
        AnimatedVisibility(
            visible = state is PaywallState.Loaded.Legacy,
            enter = fadeIn(animationSpec = defaultAnimation()),
            exit = fadeOut(animationSpec = defaultAnimation()),
        ) {
            if (state is PaywallState.Loaded.Legacy) {
                LoadedPaywall(state = state, viewModel = viewModel, isDarkTheme = isDark)
            } else {
                Logger.e(
                    "State is not loaded while transitioning animation. This may happen if state changes from " +
                        "being loaded to a different state. This should not happen.",
                )
            }
        }
    }

    // V2 Paywalls set custom fonts on the dashboard, so we don't want to use FontProvider here to set the fonts.
    AnimatedVisibility(
        visible = state is PaywallState.Loaded.Components,
        enter = fadeIn(animationSpec = defaultAnimation()),
        exit = fadeOut(animationSpec = defaultAnimation()),
    ) {
        if (state is PaywallState.Loaded.Components) {
            val paywallComponents = state.offering.paywallComponents
            if (paywallComponents != null) {
                LaunchedEffect(
                    state.offering.identifier,
                    paywallComponents.data.id,
                    paywallComponents.data.revision,
                    options.mode,
                    state.locale.toString(),
                    isDark,
                ) {
                    viewModel.trackPaywallImpressionIfNeeded()
                }
            }
            LoadedPaywallComponents(
                state = state,
                clickHandler = rememberPaywallActionHandler(viewModel),
                componentInteractionTracker = componentInteractionTracker,
            )
        } else {
            Logger.e(
                "State is not loaded while transitioning animation. This may happen if state changes " +
                    "from being loaded to a different state. This should not happen.",
            )
        }
    }

    when (state) {
        is PaywallState.Loading -> {}

        is PaywallState.Error -> {
            PaywallTheme(fontProvider = options.fontProvider) {
                ErrorDialog(
                    dismissRequest = options.dismissRequest,
                    error = state.errorMessage,
                )
            }
        }

        is PaywallState.Loaded -> {
            viewModel.actionError.value?.let {
                val errorDialog = @Composable {
                    ErrorDialog(
                        dismissRequest = viewModel::clearActionError,
                        error = it.message,
                    )
                }
                when (state) {
                    is PaywallState.Loaded.Legacy -> PaywallTheme(fontProvider = options.fontProvider) { errorDialog() }
                    is PaywallState.Loaded.Components -> errorDialog()
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LoadedPaywall(
    state: PaywallState.Loaded.Legacy,
    viewModel: PaywallViewModel,
    isDarkTheme: Boolean,
) {
    val configuration = state.configurationWithOverriddenLocale()
    val localeLanguageTags = configuration.locales.toLanguageTags()
    val offering = state.offering
    val paywallRevision = offering.paywall?.revision ?: offering.paywallComponents?.data?.revision
    val paywallIdentifier = offering.paywall?.id ?: offering.paywallComponents?.data?.id
    val honorLayoutDirection = viewModel.preferredUILocaleOverrideHonorsLayoutDirection.collectAsStateWithLifecycle().value
    LaunchedEffect(
        offering.identifier,
        paywallIdentifier,
        paywallRevision,
        state.templateConfiguration.mode,
        localeLanguageTags,
        isDarkTheme,
    ) {
        viewModel.trackPaywallImpressionIfNeeded()
    }
    val context = LocalContext.current
    val activity = context.getActivity()

    CompositionLocalProvider(
        LocalActivity provides activity,
        LocalContext provides state.contextWithConfiguration(configuration),
        LocalConfiguration provides configuration,
    ) {
        ProvideLayoutDirection(
            configuration.resolveLayoutDirection(
                editorLayoutDirection = offering.paywallComponents?.data?.componentsConfig?.base?.layoutDirection,
                honorPreferredLocaleLayoutDirection = honorLayoutDirection,
            ),
        ) {
            if (state.validationWarning != null) {
                val defaultBackground = MaterialTheme.colorScheme.background
                val defaultOnSurface = MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier.screenModeBackground(state.isInFullScreenMode, defaultBackground),
                ) {
                    DefaultPaywallView(
                        packages = state.templateConfiguration.packages.all,
                        selectedPackage = state.selectedPackage.value,
                        warning = state.validationWarning,
                        onSelectPackage = viewModel::selectPackage,
                        onPurchase = {
                            val rcPackage = state.selectedPackage.value.rcPackage
                            viewModel.trackComponentInteraction(
                                paywallPurchaseButtonAction(
                                    componentName = PaywallLegacyComponentInteraction.PURCHASE_BUTTON_NAME,
                                    componentValue = PaywallLegacyComponentInteraction.Value.IN_APP_CHECKOUT,
                                    componentUrl = null,
                                    currentPackageIdentifier = rcPackage.identifier,
                                    currentProductIdentifier = rcPackage.product.paywallProductIdentifier(),
                                ),
                            )
                            viewModel.purchaseSelectedPackage(activity)
                        },
                        onRestore = {
                            viewModel.trackComponentInteraction(
                                componentType = PaywallComponentType.BUTTON,
                                componentName = PaywallLegacyComponentInteraction.RESTORE_BUTTON_NAME,
                                componentValue = PaywallLegacyComponentInteraction.Value.RESTORE_PURCHASES,
                            )
                            viewModel.restorePurchases()
                        },
                    )
                    CloseButton(
                        shouldDisplayDismissButton = state.shouldDisplayDismissButton,
                        color = defaultOnSurface,
                        actionInProgress = viewModel.actionInProgress.value,
                        onClick = viewModel::closePaywall,
                    )
                }
            } else {
                val backgroundColor = state.templateConfiguration.getCurrentColors().background
                Box(
                    modifier = Modifier.screenModeBackground(state.isInFullScreenMode, backgroundColor),
                ) {
                    TemplatePaywall(state = state, viewModel = viewModel)
                    CloseButton(
                        shouldDisplayDismissButton = state.shouldDisplayDismissButton,
                        color = state.currentColors.closeButton,
                        actionInProgress = viewModel.actionInProgress.value,
                        onClick = viewModel::closePaywall,
                    )
                }
            }
        }
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

@Stable
@Composable
internal fun getPaywallViewModel(
    options: PaywallOptions,
    shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null,
): PaywallViewModel {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel = viewModel<PaywallViewModelImpl>(
        key = options.hashCode().toString(),
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
private fun rememberPaywallActionHandler(viewModel: PaywallViewModel): suspend (PaywallAction.External) -> Unit {
    val context: Context = LocalContext.current
    val activity: Activity? = context.getActivity()
    return remember(viewModel) {
        {
                action ->
            when (action) {
                is PaywallAction.External.RestorePurchases -> viewModel.handleRestorePurchases()
                is PaywallAction.External.PurchasePackage ->
                    if (activity == null) {
                        Logger.e("Activity is null, not initiating package purchase")
                    } else {
                        viewModel.handlePackagePurchase(
                            activity,
                            pkg = action.rcPackage,
                            resolvedOffer = action.resolvedOffer,
                        )
                    }

                is PaywallAction.External.LaunchWebCheckout -> {
                    val url = viewModel.getWebCheckoutUrl(action)
                    if (url == null) {
                        Logger.e("Web checkout URL cannot be found, not launching web checkout.")
                    } else {
                        viewModel.invalidateCustomerInfoCache()
                        context.handleUrlDestination(url, action.openMethod)
                        if (action.autoDismiss) {
                            Logger.d("Auto-dismissing paywall after launching web checkout.")
                            viewModel.closePaywall()
                        }
                    }
                }

                is PaywallAction.External.NavigateBack -> viewModel.closePaywall()
                is PaywallAction.External.WorkflowTrigger ->
                    Logger.d("Workflow received for componentId=${action.componentId} trigger=${action.triggerType}")
                is PaywallAction.External.NavigateTo -> when (val destination = action.destination) {
                    is PaywallAction.External.NavigateTo.Destination.CustomerCenter ->
                        Logger.w("Customer Center is not yet implemented on Android.")

                    is PaywallAction.External.NavigateTo.Destination.Url -> context.handleUrlDestination(
                        url = destination.url,
                        method = destination.method,
                    )
                }
            }
        }
    }
}

private fun Context.handleUrlDestination(url: String, method: ButtonComponent.UrlMethod) {
    val openingMethod = when (method) {
        ButtonComponent.UrlMethod.IN_APP_BROWSER -> URLOpeningMethod.IN_APP_BROWSER
        ButtonComponent.UrlMethod.EXTERNAL_BROWSER -> URLOpeningMethod.EXTERNAL_BROWSER
        ButtonComponent.UrlMethod.DEEP_LINK -> URLOpeningMethod.DEEP_LINK
        ButtonComponent.UrlMethod.UNKNOWN -> {
            // Buttons like this should be hidden, so this log should never be shown.
            Logger.e("Ignoring button click with unknown open method for URL: '$url'. This is a bug in the SDK.")
            return
        }
    }

    URLOpener.openURL(this, url, openingMethod)
}

private fun Modifier.screenModeBackground(isInFullScreenMode: Boolean, backgroundColor: Color): Modifier = this
    .conditional(isInFullScreenMode) {
        Modifier
            .fillMaxHeight()
            .background(backgroundColor)
    }
    .conditional(!isInFullScreenMode) {
        Modifier
            .clip(
                RoundedCornerShape(
                    topStart = UIConstant.defaultCornerRadius,
                    topEnd = UIConstant.defaultCornerRadius,
                ),
            )
            .background(backgroundColor)
    }
