package com.revenuecat.sample.main

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Package
import com.revenuecat.sample.utils.findActivity

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsState()

    if (uiState.value.shouldShowSwitchingUserDialog) {
        Dialog(onDismissRequest = { viewModel.resetSwitchUserProcess() }) {
            SwitchUserDialog(viewModel)
        }
    }

    uiState.value.displayErrorMessage?.let { errorMessage ->
        Toast.makeText(
            LocalContext.current,
            errorMessage,
            Toast.LENGTH_SHORT,
        ).show()
        viewModel.resetErrorMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Custom Entitlements Computation App",
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black,
                    )
                },
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
            )
        },
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { viewModel.initiateSwitchUserProcess() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                ) {
                    Text(text = "Switch user")
                }
                uiState.value.offerings?.current?.availablePackages?.first()?.let { rcPackage ->
                    PackageButton(
                        rcPackage,
                        "Purchase first offering",
                    ) { activity, packageToPurchase ->
                        viewModel.purchasePackage(activity, packageToPurchase)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f, true)
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Text(
                            text = uiState.value.currentCustomerInfo?.rawData?.toString(4)
                                ?: "Customer information will be displayed here",
                            fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchUserDialog(viewModel: MainViewModel) {
    var userId by remember {
        mutableStateOf("")
    }
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
                    onClick = { viewModel.resetSwitchUserProcess() },
                    content = { Text("CANCEL") },
                )
                TextButton(
                    onClick = {
                        viewModel.switchUser(userId)
                        viewModel.resetSwitchUserProcess()
                    },
                    content = { Text("OK") },
                )
            }
        }
    }
}

@Composable
private fun PackageButton(
    rcPackage: Package,
    text: String,
    modifier: Modifier = Modifier,
    onPurchaseClicked: (Activity, Package) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    Button(
        onClick = { onPurchaseClicked(context.findActivity(), rcPackage) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 32.dp),
    ) {
        Text(text = text, color = Color.White)
    }
}
