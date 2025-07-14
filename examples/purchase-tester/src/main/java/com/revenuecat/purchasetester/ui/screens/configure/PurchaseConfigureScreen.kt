package com.revenuecat.purchasetester.ui.screens.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchasetester.R
import com.revenuecat.purchasetester.ui.components.configuration.buttons.ConfigurationButtonGroup
import com.revenuecat.purchasetester.ui.components.configuration.dropdown.ConfigurationDropdown
import com.revenuecat.purchasetester.ui.components.configuration.header.ConfigurationHeader
import com.revenuecat.purchasetester.ui.components.input.PurchaseInputField
import com.revenuecat.purchasetester.ui.components.configuration.radiogroup.ConfigurationRadioGroup
import com.revenuecat.purchasetester.ui.components.configuration.radiogroup.RadioOption

@Composable
internal fun PurchaseConfigureScreen(
    modifier: Modifier = Modifier,
    viewModel: PurchaseConfigureViewModel = viewModel<PurchaseConfigureViewModelImpl>(
        factory = PurchaseConfigureViewModelImpl.Factory
    ),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToProxy: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isConfigured) {
        if (state.isConfigured) {
            onNavigateToLogin()
        }
    }

    state.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(PurchaseConfigureActions.OnErrorDismissed) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(PurchaseConfigureActions.OnErrorDismissed) }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp , vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConfigurationHeader(
            title = stringResource(R.string.sdk_configuration),
            subtitle = stringResource(R.string.sdk_configuration_description)
        )

        Column(modifier = Modifier.padding(12.dp)) {
            PurchaseInputField(
                label = stringResource(R.string.api_key),
                value = state.apiKey,
                onValueChange = {
                    viewModel.onAction(PurchaseConfigureActions.OnApiKeyChanged(it))
                },
                placeholder = stringResource(R.string.api_key_placeholder),
                icon = Icons.Default.Lock,
                modifier = Modifier.padding(bottom = 14.dp),
                enabled = !state.isLoading
            )

            PurchaseInputField(
                label = stringResource(R.string.proxy_url),
                value = state.proxyUrl,
                onValueChange = { selectedProxyUrl ->
                    viewModel.onAction(PurchaseConfigureActions.OnProxyUrlChanged(selectedProxyUrl))
                },
                placeholder = "https://your-proxy-url.com",
                icon = painterResource(id = R.drawable.ic_link),
                isOptional = true,
                modifier = Modifier.padding(bottom = 14.dp),
                enabled = !state.isLoading
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ConfigurationDropdown(
                label = stringResource(R.string.entitlement_verification),
                selectedValue = state.entitlementVerificationMode.name,
                options = EntitlementVerificationMode.entries.map { it.name },
                onValueChange = { selectedName ->
                    val mode = EntitlementVerificationMode.entries.find { it.name == selectedName }
                        ?: EntitlementVerificationMode.default
                    viewModel.onAction(PurchaseConfigureActions.OnEntitlementVerificationModeChanged(mode))
                },
                modifier = Modifier.padding(bottom = 16.dp),
                enabled = !state.isLoading
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Store Selection
            ConfigurationRadioGroup(
                label = stringResource(R.string.store),
                selectedValue = if (state.useAmazonStore) stringResource(R.string.amazon) else stringResource(R.string.google),
                options = buildList {
                    if (state.supportedStores.googleSupported) {
                        add(
                            RadioOption(
                                value = stringResource(R.string.google),
                                title = stringResource(R.string.google_play),
                                badgeColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (state.supportedStores.amazonSupported) {
                        add(
                            RadioOption(
                                value = stringResource(R.string.amazon),
                                title = stringResource(R.string.amazon_store),
                                badgeColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                },
                onValueChange = { selectedStore ->
                    val useAmazon = selectedStore == "Amazon"
                    viewModel.onAction(PurchaseConfigureActions.OnStoreChanged(useAmazon))
                },
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.padding(bottom = 16.dp),
                enabled = !state.isLoading
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Purchase Handler Selection
            ConfigurationRadioGroup(
                label = stringResource(R.string.purchases_completed_by),
                selectedValue = state.purchasesAreCompletedBy.name,
                options = listOf(
                    RadioOption(
                        value = PurchasesAreCompletedBy.REVENUECAT.name,
                        title = stringResource(R.string.revenuecat),
                        subtitle = stringResource(R.string.purchase_revenuecat_provider)
                    ),
                    RadioOption(
                        value = PurchasesAreCompletedBy.MY_APP.name,
                        title = stringResource(R.string.my_app),
                        subtitle = stringResource(R.string.purchase_my_app_provider)
                    )
                ),
                onValueChange = { selectedHandler ->
                    val completedBy = PurchasesAreCompletedBy.entries.find { it.name == selectedHandler }
                        ?: PurchasesAreCompletedBy.REVENUECAT
                    viewModel.onAction(PurchaseConfigureActions.OnPurchaseCompletionChanged(completedBy))
                },
                enabled = !state.isLoading && !state.useAmazonStore // Disable for Amazon
            )

            // Show note for Amazon store
            if (state.useAmazonStore) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Amazon store requires purchases to be completed by RevenueCat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ConfigurationButtonGroup(
            primaryButtonText = if (state.isLoading) "Configuring..." else stringResource(R.string.continue_text),
            isPrimaryEnabled = state.apiKey.isNotEmpty() && !state.isLoading,
            onPrimaryClick = { viewModel.onAction(PurchaseConfigureActions.OnContinue) },
            secondaryButtons = listOf(
                stringResource(R.string.proxy) to {
                    onNavigateToProxy()
                },
                stringResource(R.string.logs) to {
                    onNavigateToLogs()
                }
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Loading indicator
        if (state.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}