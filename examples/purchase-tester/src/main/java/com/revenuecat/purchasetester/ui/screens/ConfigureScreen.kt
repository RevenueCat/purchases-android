package com.revenuecat.purchasetester.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases_sample.BuildConfig
import com.revenuecat.purchasetester.DataStoreUtils
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.SdkConfiguration
import com.revenuecat.purchasetester.configurationDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ConfigureScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataStoreUtils = remember { DataStoreUtils(context.configurationDataStore) }
    val sdkConfig by dataStoreUtils.getSdkConfig().collectAsStateWithLifecycle(
        initialValue = SdkConfiguration("", "", false),
    )

    var apiKey by remember { mutableStateOf("") }
    var proxyUrl by remember { mutableStateOf("") }
    var selectedVerificationMode by remember { mutableStateOf(EntitlementVerificationMode.INFORMATIONAL) }
    var useAmazon by remember { mutableStateOf(false) }
    var purchasesCompletedBy by remember { mutableStateOf(PurchasesAreCompletedBy.REVENUECAT) }
    var showError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sdkConfig) {
        apiKey = sdkConfig.apiKey
        proxyUrl = sdkConfig.proxyUrl ?: ""
        useAmazon = sdkConfig.useAmazon
    }

    val supportedStores = BuildConfig.SUPPORTED_STORES.split(",")
    val googleAvailable = supportedStores.contains("google")
    val amazonAvailable = supportedStores.contains("amazon")

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onNavigateToProxy) {
                        Text("Proxy")
                    }
                    TextButton(onClick = onNavigateToLogs) {
                        Text("Logs")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("😻", style = MaterialTheme.typography.displayLarge)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "SDK Configuration",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = proxyUrl,
                onValueChange = { proxyUrl = it },
                label = { Text("Proxy URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Entitlement Verification", style = MaterialTheme.typography.labelLarge)
            Column {
                EntitlementVerificationMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedVerificationMode == mode,
                            onClick = { selectedVerificationMode = mode },
                        )
                        Text(mode.name)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Store", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !useAmazon,
                        onClick = { useAmazon = false },
                        enabled = googleAvailable,
                    )
                    Text("Google")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = useAmazon,
                        onClick = { useAmazon = true },
                        enabled = amazonAvailable,
                    )
                    Text("Amazon")
                }
            }

            if (!googleAvailable) {
                Text("Google store is unavailable", color = MaterialTheme.colorScheme.error)
            }
            if (!amazonAvailable) {
                Text("Amazon store is unavailable", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Purchases Completed By", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = purchasesCompletedBy == PurchasesAreCompletedBy.REVENUECAT,
                        onClick = { purchasesCompletedBy = PurchasesAreCompletedBy.REVENUECAT },
                        enabled = !useAmazon,
                    )
                    Text("RevenueCat")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = purchasesCompletedBy == PurchasesAreCompletedBy.MY_APP,
                        onClick = { purchasesCompletedBy = PurchasesAreCompletedBy.MY_APP },
                        enabled = !useAmazon,
                    )
                    Text("My App")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        showError = "API Key is empty"
                        return@Button
                    }
                    scope.launch {
                        configureSdk(
                            context = context,
                            apiKey = apiKey,
                            proxyUrl = proxyUrl,
                            verificationMode = selectedVerificationMode,
                            useAmazon = useAmazon,
                            purchasesCompletedBy = purchasesCompletedBy,
                            dataStoreUtils = dataStoreUtils,
                        )
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }

    if (showError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showError = "" },
            title = { Text("Error") },
            text = { Text(showError) },
            confirmButton = {
                TextButton(onClick = { showError = "" }) {
                    Text("OK")
                }
            },
        )
    }
}

@Suppress("LongParameterList")
private suspend fun configureSdk(
    context: Context,
    apiKey: String,
    proxyUrl: String,
    verificationMode: EntitlementVerificationMode,
    useAmazon: Boolean,
    purchasesCompletedBy: PurchasesAreCompletedBy,
    dataStoreUtils: DataStoreUtils,
) {
    val application = context.applicationContext as MainApplication

    if (proxyUrl.isNotEmpty()) {
        try {
            Purchases.proxyURL = java.net.URL(proxyUrl)
        } catch (e: java.net.MalformedURLException) {
            android.util.Log.w("ConfigureScreen", "Invalid proxy URL: $proxyUrl", e)
        }
    }

    Purchases.logLevel = com.revenuecat.purchases.LogLevel.VERBOSE

    val configurationBuilder = if (useAmazon) {
        com.revenuecat.purchases.amazon.AmazonConfiguration.Builder(application, apiKey)
    } else {
        com.revenuecat.purchases.PurchasesConfiguration.Builder(application, apiKey)
    }

    val configuration = configurationBuilder
        .diagnosticsEnabled(true)
        .entitlementVerificationMode(verificationMode)
        .purchasesAreCompletedBy(purchasesCompletedBy)
        .pendingTransactionsForPrepaidPlansEnabled(true)
        .build()
    Purchases.configure(configuration)

    Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))
    Purchases.sharedInstance.updatedCustomerInfoListener = application

    dataStoreUtils.saveSdkConfig(SdkConfiguration(apiKey, proxyUrl, useAmazon))
}
