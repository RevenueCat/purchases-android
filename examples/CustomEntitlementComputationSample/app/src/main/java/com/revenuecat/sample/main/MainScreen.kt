package com.revenuecat.sample.main

import CustomerInfoDetailScreen
import CustomerInfoEventsList
import ExplanationScreen
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.purchases.Package
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme
import com.revenuecat.sample.utils.findActivity

@Composable
fun MainScreenNavigation() {
    val viewModel: MainViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsState()

    val navController = rememberNavController()
    NavHost(navController, startDestination = "main") {
        composable("main") { MainScreen(navController = navController, viewModel = viewModel) }
        composable("customerInfoDetails/{id}") { backStackEntry ->
            val event = uiState.value.customerInfoList.find {
                it.id.toString() == backStackEntry.arguments?.getString("id")
            }

            if (event != null) {
                CustomerInfoDetailScreen(event = event)
            } else {
                Text("Could not find event with id ${backStackEntry.arguments?.getString("id")}")
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
    uiState: State<MainState> = viewModel.uiState.collectAsState()
) {

    if (uiState.value.shouldShowSwitchingUserDialog) {
        Dialog(onDismissRequest = { viewModel.resetSwitchUserProcess() }) {
            SwitchUserDialog(viewModel)
        }
    }

    if (uiState.value.shouldShowExplanationDialog) {
        Dialog(onDismissRequest = { viewModel.dismissExplanationDialog() }) {
            ExplanationScreen(onDismiss = { viewModel.dismissExplanationDialog() })
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
                    onClick = {
                        viewModel.showExplanationDialog()
                    },
                    modifier = Modifier.wrapContentSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),
                ) {
                    Column {
                        Text(
                            text = "This app uses RevenueCat under CustomEntitlementsComputation mode.",
                            modifier = Modifier
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = "Tap here for more details about this mode.",
                            modifier = Modifier
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Current App User ID: ")
                    }
                    append(uiState.value.currentAppUserID)
                })

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
                Box(
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        Text(
                            text = "CustomerInfo listener values",
                            fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "List fills in below when new values arrive",
                            fontSize = 14.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                        )
                        CustomerInfoEventsList(
                            uiState.value.customerInfoList,
                            onEventClicked = { customerInfoEvent ->
                                navController.navigate("customerInfoDetails/${customerInfoEvent.id}")
                            },
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

@Preview
@Composable
fun MainScreenPreview() {
    CustomEntitlementComputationTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "main") {
            composable("main") {
                MainScreen(navController = navController)
            }
        }
    }
}
