package com.revenuecat.rcttester.ui.configuration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revenuecat.rcttester.config.PurchaseLogicType
import com.revenuecat.rcttester.config.PurchasesCompletedByType
import com.revenuecat.rcttester.config.SDKConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    initialConfiguration: SDKConfiguration,
    onConfigure: (SDKConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKey by remember { mutableStateOf(initialConfiguration.apiKey) }
    var appUserID by remember { mutableStateOf(initialConfiguration.appUserID) }
    var purchasesAreCompletedBy by remember {
        mutableStateOf(initialConfiguration.purchasesAreCompletedBy)
    }
    var purchaseLogic by remember {
        mutableStateOf(initialConfiguration.purchaseLogic)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var purchaseLogicDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier) { paddingValues ->
        ConfigurationScreenContent(
            state = ConfigurationScreenState(
                apiKey = apiKey,
                appUserID = appUserID,
                purchasesAreCompletedBy = purchasesAreCompletedBy,
                purchaseLogic = purchaseLogic,
                dropdownExpanded = dropdownExpanded,
                purchaseLogicDropdownExpanded = purchaseLogicDropdownExpanded,
            ),
            callbacks = ConfigurationScreenCallbacks(
                onApiKeyChange = { apiKey = it },
                onAppUserIDChange = { appUserID = it },
                onPurchasesAreCompletedByChange = { purchasesAreCompletedBy = it },
                onPurchaseLogicChange = { purchaseLogic = it },
                onDropdownExpandedChange = { dropdownExpanded = it },
                onPurchaseLogicDropdownExpandedChange = { purchaseLogicDropdownExpanded = it },
                onConfigure = {
                    val sanitizedApiKey = apiKey.trim().replace("\n", "").replace("\r", "")
                    onConfigure(
                        SDKConfiguration(
                            apiKey = sanitizedApiKey,
                            appUserID = appUserID.trim(),
                            purchasesAreCompletedBy = purchasesAreCompletedBy,
                            purchaseLogic = purchaseLogic,
                        ),
                    )
                },
            ),
            paddingValues = paddingValues,
        )
    }
}

private data class ConfigurationScreenState(
    val apiKey: String,
    val appUserID: String,
    val purchasesAreCompletedBy: PurchasesCompletedByType,
    val purchaseLogic: PurchaseLogicType,
    val dropdownExpanded: Boolean,
    val purchaseLogicDropdownExpanded: Boolean,
)

private data class ConfigurationScreenCallbacks(
    val onApiKeyChange: (String) -> Unit,
    val onAppUserIDChange: (String) -> Unit,
    val onPurchasesAreCompletedByChange: (PurchasesCompletedByType) -> Unit,
    val onPurchaseLogicChange: (PurchaseLogicType) -> Unit,
    val onDropdownExpandedChange: (Boolean) -> Unit,
    val onPurchaseLogicDropdownExpandedChange: (Boolean) -> Unit,
    val onConfigure: () -> Unit,
)

@Composable
private fun ConfigurationScreenContent(
    state: ConfigurationScreenState,
    callbacks: ConfigurationScreenCallbacks,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
    ) {
        ApiKeySection(
            apiKey = state.apiKey,
            onApiKeyChange = callbacks.onApiKeyChange,
        )
        AppUserIDSection(
            appUserID = state.appUserID,
            onAppUserIDChange = callbacks.onAppUserIDChange,
        )
        PurchasesCompletedBySection(
            purchasesAreCompletedBy = state.purchasesAreCompletedBy,
            dropdownExpanded = state.dropdownExpanded,
            onPurchasesAreCompletedByChange = callbacks.onPurchasesAreCompletedByChange,
            onDropdownExpandedChange = callbacks.onDropdownExpandedChange,
        )
        if (state.purchasesAreCompletedBy == PurchasesCompletedByType.MY_APP) {
            PurchaseLogicSection(
                purchaseLogic = state.purchaseLogic,
                dropdownExpanded = state.purchaseLogicDropdownExpanded,
                onPurchaseLogicChange = callbacks.onPurchaseLogicChange,
                onDropdownExpandedChange = callbacks.onPurchaseLogicDropdownExpandedChange,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ConfigureButton(
            apiKey = state.apiKey,
            onConfigure = callbacks.onConfigure,
        )
    }
}

@Composable
private fun ApiKeySection(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "RevenueCat API Key",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Text(
            text = "Your RevenueCat API key. Can also be set via BuildConfig.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun AppUserIDSection(
    appUserID: String,
    onAppUserIDChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "App User ID",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = appUserID,
            onValueChange = onAppUserIDChange,
            label = { Text("App User ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Text(
            text = "Leave empty to let the SDK generate an anonymous user ID.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchasesCompletedBySection(
    purchasesAreCompletedBy: PurchasesCompletedByType,
    dropdownExpanded: Boolean,
    onPurchasesAreCompletedByChange: (PurchasesCompletedByType) -> Unit,
    onDropdownExpandedChange: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = "Purchases Are Completed By",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = onDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = purchasesAreCompletedBy.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text("Purchases Completed By") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { onDropdownExpandedChange(false) },
            ) {
                PurchasesCompletedByType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onPurchasesAreCompletedByChange(option)
                            onDropdownExpandedChange(false)
                        },
                    )
                }
            }
        }
        Text(
            text = purchasesCompletedByFooter(purchasesAreCompletedBy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun ConfigureButton(
    apiKey: String,
    onConfigure: () -> Unit,
) {
    Button(
        onClick = onConfigure,
        enabled = apiKey.trim().isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Configure SDK")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseLogicSection(
    purchaseLogic: PurchaseLogicType,
    dropdownExpanded: Boolean,
    onPurchaseLogicChange: (PurchaseLogicType) -> Unit,
    onDropdownExpandedChange: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = "Purchase Logic",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = onDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = purchaseLogic.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text("Purchase Logic") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { onDropdownExpandedChange(false) },
            ) {
                PurchaseLogicType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onPurchaseLogicChange(option)
                            onDropdownExpandedChange(false)
                        },
                    )
                }
            }
        }
        Text(
            text = purchaseLogicFooter(purchaseLogic),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

private fun purchaseLogicFooter(option: PurchaseLogicType): String {
    return when (option) {
        PurchaseLogicType.THROUGH_REVENUECAT ->
            "The app still uses the Purchases purchase methods to make purchases."
        PurchaseLogicType.USING_BILLING_CLIENT_DIRECTLY ->
            "The app takes care of making the purchases using BillingClient APIs directly."
    }
}

private fun purchasesCompletedByFooter(option: PurchasesCompletedByType): String {
    return when (option) {
        PurchasesCompletedByType.REVENUECAT ->
            "All purchases are done through RevenueCat's purchase methods. " +
                "RevenueCat will also finish transactions after successful receipt posting."
        PurchasesCompletedByType.MY_APP ->
            "The app is responsible for finishing transactions (also known as 'Observer Mode')."
    }
}
