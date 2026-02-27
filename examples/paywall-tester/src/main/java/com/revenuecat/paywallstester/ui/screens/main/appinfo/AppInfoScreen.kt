package com.revenuecat.paywallstester.ui.screens.main.appinfo

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.Constants
import com.revenuecat.paywallstester.MainActivity
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreenViewModel.UiState
import com.revenuecat.paywallstester.ui.screens.main.createCustomerCenterListener
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.ui.revenuecatui.views.CustomerCenterView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

@SuppressWarnings("LongMethod")
@Composable
fun AppInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: AppInfoScreenViewModel = viewModel<AppInfoScreenViewModelImpl>(
        factory = AppInfoScreenViewModelImpl.Factory,
    ),
    tappedOnCustomerCenter: () -> Unit,
) {
    var isDebugBottomSheetVisible by remember { mutableStateOf(false) }
    var showLogInDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCustomerCenterView by remember { mutableStateOf(false) }
    var isClearingFileCache by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val perViewListener = remember(context) {
        createCustomerCenterListener(
            tag = "CustomerCenterView",
            onCustomAction = { actionIdentifier, purchaseIdentifier ->
                val message = buildString {
                    append("Custom action from view: ")
                    append(actionIdentifier)
                    purchaseIdentifier?.let {
                        append(" (product: $it)")
                    }
                }
                Log.d("CustomerCenterView", "Per-view listener received custom action: $message")
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val state by viewModel.state.collectAsState()
        val activity = LocalContext.current as MainActivity
        val currentUserID by remember { derivedStateOf { state.appUserID } }
        val currentApiKeyDescription by remember { derivedStateOf { state.apiKeyDescription } }
        val currentActiveEntitlements by remember { derivedStateOf { state.activeEntitlements } }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "Current user ID: $currentUserID")
        Text(text = "Current active entitlements: $currentActiveEntitlements")
        Text(text = "Current API key: $currentApiKeyDescription")
        Button(onClick = { showLogInDialog = true }) {
            Text(text = "Log in")
        }
        Button(onClick = { viewModel.logOut() }) {
            Text(text = "Log out")
        }
        Button(onClick = { showApiKeyDialog = true }) {
            Text(text = "Switch API key")
        }
        Button(
            enabled = !isClearingFileCache,
            onClick = {
                isClearingFileCache = true
                coroutineScope.launch {
                    val message = try {
                        val clearResult = withContext(Dispatchers.IO) {
                            clearPaywallFileCache(context)
                        }
                        clearResult.message
                    } catch (@Suppress("TooGenericExceptionCaught") throwable: Throwable) {
                        Log.e("PaywallTester", "Failed to clear paywall file cache", throwable)
                        "Failed to clear paywall file cache. Check logs."
                    } finally {
                        isClearingFileCache = false
                    }

                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
        ) {
            Text(text = if (isClearingFileCache) "Clearing file cache..." else "Clear file cache")
        }
        Button(onClick = { isDebugBottomSheetVisible = true }) {
            Text(text = "Show debug view")
        }
        Button(onClick = {
            tappedOnCustomerCenter()
        }) {
            Text(text = "Show customer center")
        }
        Button(onClick = {
            showCustomerCenterView = true
        }) {
            Text(text = "Customer Center (per-view Listener)")
        }
        Button(onClick = {
            activity.launchCustomerCenter()
        }) {
            Text(text = "Customer Center (Activity)")
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.refresh() }) {
            Text(text = "Refresh")
        }
    }

    if (showLogInDialog) {
        LoginDialog(viewModel) { showLogInDialog = false }
    }
    if (showApiKeyDialog) {
        ApiKeyDialog(
            onApiKeyClick = {
                viewModel.switchApiKey(it)
                showApiKeyDialog = false
            },
        ) { showApiKeyDialog = false }
    }

    if (showCustomerCenterView) {
        Dialog(
            onDismissRequest = { showCustomerCenterView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    CustomerCenterView(
                        context = viewContext,
                        dismissHandler = { showCustomerCenterView = false },
                        customerCenterListener = perViewListener,
                    )
                },
            )
        }
    }

    DebugRevenueCatBottomSheet(
        onPurchaseCompleted = { isDebugBottomSheetVisible = false },
        onPurchaseErrored = { Log.e("PaywallTester", "Error purchasing through debug view: $it") },
        isVisible = isDebugBottomSheetVisible,
        onDismissCallback = { isDebugBottomSheetVisible = false },
    )
}

@Composable
private fun LoginDialog(viewModel: AppInfoScreenViewModel, onDismissed: () -> Unit) {
    var userId by remember {
        mutableStateOf("")
    }
    Dialog(onDismissRequest = { onDismissed() }) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column {
                Column(Modifier.padding(24.dp)) {
                    Text(text = "Enter user ID")
                    Spacer(Modifier.size(16.dp))
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("User ID") },
                    )
                }
                Spacer(Modifier.size(4.dp))
                Row(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(
                        onClick = { onDismissed() },
                        content = { Text("CANCEL") },
                    )
                    TextButton(
                        onClick = {
                            viewModel.logIn(userId)
                            onDismissed()
                        },
                        content = { Text("OK") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(onApiKeyClick: (String) -> Unit, onDismissed: () -> Unit) {
    Dialog(onDismissRequest = { onDismissed() }) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(all = 16.dp)) {
                ApiKeyButton(
                    label = Constants.GOOGLE_API_KEY_A_LABEL,
                    apiKey = Constants.GOOGLE_API_KEY_A,
                    onClick = onApiKeyClick,
                )

                ApiKeyButton(
                    label = Constants.GOOGLE_API_KEY_B_LABEL,
                    apiKey = Constants.GOOGLE_API_KEY_B,
                    onClick = onApiKeyClick,
                )

                Spacer(Modifier.size(4.dp))
                Row(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = { onDismissed() }) {
                        Text("CANCEL")
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyButton(label: String, apiKey: String, onClick: (String) -> Unit) {
    TextButton(onClick = { onClick(apiKey) }) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(text = apiKey)
        }
    }
}

private data class FileCacheClearResult(
    val deletedEntries: Int,
    val failedEntries: Int,
) {
    val message: String
        get() = when {
            failedEntries > 0 -> "Cleared paywall file cache with $failedEntries deletion error(s)."
            deletedEntries == 0 -> "Paywall file cache was already empty."
            else -> "Cleared paywall file cache."
        }
}

private fun clearPaywallFileCache(context: Context): FileCacheClearResult {
    val paywallCacheDirectories = listOf(
        File(context.cacheDir, "rc_files"),
        File(context.cacheDir, "rc_paywall_fonts"),
    )

    var deletedEntries = 0
    var failedEntries = 0

    paywallCacheDirectories.forEach { directory ->
        if (directory.deleteRecursively()) {
            deletedEntries += 1
        } else {
            failedEntries += 1
            Log.w("PaywallTester", "Failed to delete cache entry: ${directory.absolutePath}")
        }
    }

    return FileCacheClearResult(
        deletedEntries = deletedEntries,
        failedEntries = failedEntries,
    )
}

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
fun AppInfoScreenPreview() {
    AppInfoScreen(
        viewModel = object : AppInfoScreenViewModel {
            override val state: StateFlow<UiState>
                get() = MutableStateFlow(
                    UiState(
                        appUserID = "test-user-id",
                        apiKeyDescription = "test-api-key",
                        activeEntitlements = listOf("pro", "premium"),
                    ),
                )

            override fun logIn(newAppUserId: String) { }
            override fun logOut() { }
            override fun switchApiKey(newApiKey: String) { }
            override fun refresh() { }
        },
        tappedOnCustomerCenter = {},
    )
}

@Preview
@Composable
private fun ApiKeyDialog_Preview() {
    ApiKeyDialog(
        onApiKeyClick = {},
        onDismissed = {},
    )
}
