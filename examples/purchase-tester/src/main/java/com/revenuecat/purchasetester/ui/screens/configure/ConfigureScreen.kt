package com.revenuecat.purchasetester.ui.screens.configure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases_sample.BuildConfig
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme
import kotlinx.coroutines.launch

private object ConfigureScreenDefaults {
    val TITLE_EMOJI_SIZE: TextUnit = 100.sp
    val VERTICAL_SPACING: Dp = 24.dp
    val SMALL_SPACING: Dp = 10.dp
    val TOP_PADDING: Dp = 30.dp
    val BOTTOM_PADDING: Dp = 25.dp
    const val BUTTON_WIDTH_FRACTION = 0.40f
    const val ROW_WEIGHT_LABEL = 0.5f
    const val ROW_WEIGHT_INPUT = 0.5f
}

private val TransparentTextFieldColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

@Composable
fun ConfigureScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
) {
    ConfigureScreenContent(
        onLogsClick = onNavigateToLogs,
        onProxyClick = onNavigateToProxy,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun ConfigureScreenContent(
    onLogsClick: () -> Unit,
    onProxyClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    configureScreenViewModel: ConfigureScreenViewModel = viewModel<ConfigureScreenViewModelImpl>(
        factory = ConfigureScreenViewModelImpl.Factory
    ),
) {
    val viewModelState by configureScreenViewModel.state.collectAsStateWithLifecycle()

    var apiKey by remember { mutableStateOf("") }
    var proxyUrl by remember { mutableStateOf("") }
    var entitlementVerificationMode by remember { mutableStateOf(EntitlementVerificationMode.INFORMATIONAL) }
    val coroutineScope = rememberCoroutineScope()
    var selectedStore by remember { mutableStateOf(StoreType.GOOGLE) }
    var purchasesAreCompletedBy by remember { mutableStateOf(PurchasesAreCompletedBy.REVENUECAT) }
    var isContinueButtonEnabled by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val showErrorDialog = errorMessage != null

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        configureScreenViewModel.events.collect { event ->
            when (event) {
                is ConfigureUiEvent.Error -> {
                    if (event.message.isNotEmpty()) {
                        errorMessage = event.message
                        isContinueButtonEnabled = true
                    }
                }

                ConfigureUiEvent.Success -> {
                    isContinueButtonEnabled = true
                    onNavigateToLogin()
                }
            }
        }
    }

    LaunchedEffect(viewModelState) {
        if (viewModelState is ConfigureScreenState.ConfigureScreenData) {
            val data = viewModelState as ConfigureScreenState.ConfigureScreenData
            apiKey = data.apiKey
            proxyUrl = data.proxyUrl
            entitlementVerificationMode = data.entitlementVerificationMode
            selectedStore = data.selectedStoreType
            purchasesAreCompletedBy = data.purchasesAreCompletedBy
            configureScreenViewModel.saveProxyUrl(proxyUrl)
        }
    }

    LaunchedEffect(selectedStore) {
        if (selectedStore == StoreType.AMAZON && purchasesAreCompletedBy != PurchasesAreCompletedBy.REVENUECAT) {
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
            configureScreenViewModel.savePurchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
        }
    }

    val supportedStores = remember { BuildConfig.SUPPORTED_STORES.split(",").map { it.trim().lowercase() } }
    val isGoogleStoreEnabled = supportedStores.contains(StoreType.GOOGLE.name.lowercase())
    val isAmazonStoreEnabled = supportedStores.contains(StoreType.AMAZON.name.lowercase())

    LaunchedEffect(isGoogleStoreEnabled, isAmazonStoreEnabled) {
        when {
            selectedStore == StoreType.GOOGLE && !isGoogleStoreEnabled && isAmazonStoreEnabled -> {
                selectedStore = StoreType.AMAZON
            }

            selectedStore == StoreType.AMAZON && !isAmazonStoreEnabled && isGoogleStoreEnabled -> {
                selectedStore = StoreType.GOOGLE
            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(text = "Error") },
            text = { Text(text = errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .safeDrawingPadding()
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.TOP_PADDING))
            Text(
                text = stringResource(R.string.loving_cat),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = ConfigureScreenDefaults.TITLE_EMOJI_SIZE),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.sdk_configuration),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            LabeledInputRow(label = stringResource(R.string.api_key)) {
                TextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        configureScreenViewModel.saveApiKey(it)
                    },
                    colors = TransparentTextFieldColors,
                )
            }
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            LabeledInputRow(label = stringResource(R.string.proxy_url_optional)) {
                TextField(
                    value = proxyUrl,
                    onValueChange = {
                        proxyUrl = it
                        configureScreenViewModel.saveProxyUrl(it)
                    },
                    colors = TransparentTextFieldColors,
                )
            }
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            LabeledInputRow(label = stringResource(R.string.entitlement_verification)) {
                EntitlementVerificationDropdownMenu(
                    selected = entitlementVerificationMode,
                    onClick = { selected ->
                        entitlementVerificationMode = selected
                        configureScreenViewModel.saveEntitlementVerificationMode(selected)
                    }
                )
            }
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            Text(
                text = stringResource(R.string.store),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.SMALL_SPACING))
            StoreSelectionRadioButtons(
                selected = selectedStore,
                onStoreSelected = {
                    selectedStore = it
                    configureScreenViewModel.saveStoreType(it)

                    if (it == StoreType.AMAZON) {
                        purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
                        configureScreenViewModel.savePurchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                    }
                },
                isGoogleStoreEnabled = isGoogleStoreEnabled,
                isAmazonStoreEnabled = isAmazonStoreEnabled
            )
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            Text(
                text = stringResource(R.string.purchases_completed_by),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.SMALL_SPACING))
            PurchaseCompletionRadioButtons(
                selected = purchasesAreCompletedBy,
                enabled = selectedStore != StoreType.AMAZON,
                onPurchaseCompletionTypeSelected = {
                    purchasesAreCompletedBy = it
                    configureScreenViewModel.savePurchasesAreCompletedBy(it)
                }
            )

            if (!isGoogleStoreEnabled) {
                StoreUnavailableMessage(stringResource(R.string.google_store_is_unavailable))
            }

            if (!isAmazonStoreEnabled) {
                StoreUnavailableMessage(stringResource(R.string.amazon_store_is_unavailable))
            }

            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))

            Button(
                modifier = Modifier.fillMaxWidth(ConfigureScreenDefaults.BUTTON_WIDTH_FRACTION),
                enabled = isContinueButtonEnabled,
                onClick = {
                    val validation = configureScreenViewModel.validateInputs(apiKey, proxyUrl)
                    if (validation is ValidationResult.Invalid) {
                        errorMessage = validation.message
                        return@Button
                    }

                    isContinueButtonEnabled = false

                    coroutineScope.launch {
                        val application = context.applicationContext as? MainApplication
                        if (application == null) {
                            errorMessage = "Application initialization error"
                            isContinueButtonEnabled = true
                            return@launch
                        }

                        configureScreenViewModel.configureSDK(application)
                    }
                }
            ) {
                Text(stringResource(R.string.continue_text))
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.Bottom
            ) {
                Button(onClick = { onProxyClick() }) {
                    Text(text = stringResource(R.string.proxy))
                }
                Spacer(modifier = Modifier.width(ConfigureScreenDefaults.SMALL_SPACING))
                Button(onClick = { onLogsClick() }) {
                    Text(text = stringResource(R.string.logs))
                }
            }

            Spacer(modifier = Modifier.height(ConfigureScreenDefaults.BOTTOM_PADDING))
        }
    }
}

@Composable
private fun LabeledInputRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier
                .weight(ConfigureScreenDefaults.ROW_WEIGHT_LABEL)
                .align(Alignment.CenterVertically),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Box(
            modifier = Modifier
                .weight(ConfigureScreenDefaults.ROW_WEIGHT_INPUT)
                .align(Alignment.CenterVertically)
        ) {
            content()
        }
    }
}

@Composable
private fun StoreUnavailableMessage(message: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
    }
    Spacer(modifier = Modifier.height(ConfigureScreenDefaults.VERTICAL_SPACING))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntitlementVerificationDropdownMenu(
    selected: EntitlementVerificationMode,
    onClick: (EntitlementVerificationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(fontSize = 14.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TransparentTextFieldColors,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            enumValues<EntitlementVerificationMode>().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(text = mode.name) },
                    onClick = {
                        onClick(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StoreSelectionRadioButtons(
    selected: StoreType,
    onStoreSelected: (StoreType) -> Unit,
    isGoogleStoreEnabled: Boolean = true,
    isAmazonStoreEnabled: Boolean = true,
) {
    val radioOptions = StoreType.entries.toTypedArray()

    Row(
        modifier = Modifier
            .selectableGroup()
            .fillMaxWidth()
    ) {
        radioOptions.forEach { storeType ->
            val isEnabled = when (storeType) {
                StoreType.GOOGLE -> isGoogleStoreEnabled
                StoreType.AMAZON -> isAmazonStoreEnabled
            }

            Row(
                Modifier.selectable(
                    selected = (storeType == selected),
                    onClick = {
                        if (isEnabled) {
                            onStoreSelected(storeType)
                        }
                    },
                    enabled = isEnabled,
                    role = Role.RadioButton
                ),
            ) {
                RadioButton(
                    selected = (storeType == selected),
                    onClick = null,
                    enabled = isEnabled
                )
                Text(
                    text = storeType.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 7.dp),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Spacer(modifier = Modifier.width(ConfigureScreenDefaults.VERTICAL_SPACING))
            }
        }
    }
}

@Composable
fun PurchaseCompletionRadioButtons(
    selected: PurchasesAreCompletedBy,
    enabled: Boolean = true,
    onPurchaseCompletionTypeSelected: (PurchasesAreCompletedBy) -> Unit,
) {
    val radioOptions = PurchasesAreCompletedBy.entries.toTypedArray()
    Row(
        modifier = Modifier
            .selectableGroup()
            .fillMaxWidth()
    ) {
        radioOptions.forEach { option ->
            Row(
                Modifier.selectable(
                    selected = (option == selected),
                    onClick = {
                        if (enabled) {
                            onPurchaseCompletionTypeSelected(option)
                        }
                    },
                    enabled = enabled,
                    role = Role.RadioButton
                ),
            ) {
                RadioButton(
                    selected = (option == selected),
                    onClick = null,
                    enabled = enabled
                )
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 7.dp),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Spacer(modifier = Modifier.width(ConfigureScreenDefaults.VERTICAL_SPACING))
            }
        }
    }
}

@Preview(showBackground = true, name = "Configure Screen - With Data")
@Composable
private fun ConfigureScreenPreview() {
    PurchaseTesterTheme {
        ConfigureScreenContent(
            onLogsClick = {},
            onProxyClick = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "Configure Screen - Dark Theme")
@Composable
private fun ConfigureScreenPreview_Dark() {
    PurchaseTesterTheme(darkTheme = true) {
        ConfigureScreenContent(
            onLogsClick = {},
            onProxyClick = {},
            onNavigateToLogin = {}
        )
    }
}
