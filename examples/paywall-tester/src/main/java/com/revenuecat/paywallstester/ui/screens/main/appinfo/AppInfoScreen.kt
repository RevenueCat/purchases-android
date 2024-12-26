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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatBottomSheet
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
fun AppInfoScreen(viewModel: AppInfoScreenViewModel = viewModel<AppInfoScreenViewModelImpl>()) {
    var isDebugBottomSheetVisible by remember { mutableStateOf(false) }
    var isCustomerCenterVisible by remember { mutableStateOf(false) }
    var showLogInDialog by remember { mutableStateOf(false) }

    if (isCustomerCenterVisible) {
        CustomerCenter(modifier = Modifier.fillMaxSize()) {
            isCustomerCenterVisible = false
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val currentUserID = viewModel.state.collectAsState().value ?: "No user logged in"
        Text(text = "Current user ID: $currentUserID")
        Button(onClick = { showLogInDialog = true }) {
            Text(text = "Log in")
        }
        Button(onClick = { viewModel.logOut() }) {
            Text(text = "Log out")
        }
        Button(onClick = { isDebugBottomSheetVisible = true }) {
            Text(text = "Show debug view")
        }
        Button(onClick = { isCustomerCenterVisible = true }) {
            Text(text = "Show customer center")
        }
    }

    if (showLogInDialog) {
        LoginDialog(viewModel) { showLogInDialog = false }
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

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
fun AppInfoScreenPreview() {
    AppInfoScreen(
        viewModel = object : AppInfoScreenViewModel {
            override val state: StateFlow<String?>
                get() = MutableStateFlow("test-user-id")

            override fun logIn(newAppUserId: String) { }
            override fun logOut() { }
        },
    )
}
