package com.revenuecat.paywallstester.ui.screens.main.appinfo

import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.Constants
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreenViewModel.UiState
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "CustomerCenterTest"

@SuppressWarnings("LongMethod")
@Composable
fun AppInfoScreen(
    viewModel: AppInfoScreenViewModel = viewModel<AppInfoScreenViewModelImpl>(
        factory = AppInfoScreenViewModelImpl.Factory,
    ),
) {
    var isDebugBottomSheetVisible by remember { mutableStateOf(false) }
    var isCustomerCenterVisible by rememberSaveable { mutableStateOf(false) }
    var showLogInDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Use remember to cache the listener across recompositions
    val customerCenterListener = remember { createCustomerCenterListener() }

    if (isCustomerCenterVisible) {
        CustomerCenter(
            modifier = Modifier.fillMaxSize(),
            options = CustomerCenterOptions.Builder()
                .setListener(customerCenterListener)
                .build(),
        ) {
            isCustomerCenterVisible = false
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val state by viewModel.state.collectAsState()
        val currentUserID by remember { derivedStateOf { state.appUserID } }
        val currentApiKeyDescription by remember { derivedStateOf { state.apiKeyDescription } }
        Text(text = "Current user ID: $currentUserID")
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
        Button(onClick = { isDebugBottomSheetVisible = true }) {
            Text(text = "Show debug view")
        }
        Button(onClick = {
            isCustomerCenterVisible = true
        }) {
            Text(text = "Show customer center")
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

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
fun AppInfoScreenPreview() {
    AppInfoScreen(
        viewModel = object : AppInfoScreenViewModel {
            override val state: StateFlow<UiState>
                get() = MutableStateFlow(UiState(appUserID = "test-user-id", apiKeyDescription = "test-api-key"))

            override fun logIn(newAppUserId: String) { }
            override fun logOut() { }
            override fun switchApiKey(newApiKey: String) { }
        },
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

private fun createCustomerCenterListener(): CustomerCenterListener {
    return object : CustomerCenterListener {
        override fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
            Log.d(TAG, "Local listener: onManagementOptionSelected called with action: $action")
        }

        override fun onRestoreStarted() {
            Log.d(TAG, "Local listener: onRestoreStarted called")
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            Log.d(
                TAG,
                "Local listener: onRestoreCompleted called with customer info: " +
                    customerInfo.originalAppUserId,
            )
        }

        override fun onRestoreFailed(error: PurchasesError) {
            Log.d(TAG, "Local listener: onRestoreFailed called with error: ${error.message}")
        }

        override fun onShowingManageSubscriptions() {
            Log.d(TAG, "Local listener: onShowingManageSubscriptions called")
        }

        override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
            Log.d(TAG, "Local listener: onFeedbackSurveyCompleted called with option ID: $feedbackSurveyOptionId")
        }
    }
}
