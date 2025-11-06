package com.revenuecat.purchasetester.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchasetester.OverviewViewModel

@Suppress("LongMethod", "LongParameterList")
@Composable
fun OverviewScreen(
    webPurchaseRedemption: WebPurchaseRedemption?,
    onWebPurchaseRedemptionConsume: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToOffering: (String) -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = viewModel(),
) {
    val context = LocalContext.current
    val customerInfo by viewModel.customerInfo.collectAsState(null)
    val activeEntitlements by viewModel.activeEntitlements.collectAsState("")
    val allEntitlements by viewModel.allEntitlements.collectAsState("")
    val verificationResult by viewModel.verificationResult.collectAsState(VerificationResult.NOT_REQUESTED)
    val isRestoring by viewModel.isRestoring.collectAsState(false)
    var offerings by remember { mutableStateOf<Offerings?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.retrieveCustomerInfo()
        Purchases.sharedInstance.getOfferingsWith(
            { error -> showError = error.message },
            { offerings = it },
        )
    }

    LaunchedEffect(webPurchaseRedemption) {
        val redemption = webPurchaseRedemption
        val consumeCallback = onWebPurchaseRedemptionConsume
        redemption?.let {
            Purchases.sharedInstance.redeemWebPurchase(it) { result ->
                when (result) {
                    is RedeemWebPurchaseListener.Result.Success -> {
                        showError = "Successfully redeemed web purchase"
                    }
                    is RedeemWebPurchaseListener.Result.Error -> {
                        showError = result.error.message
                    }
                    RedeemWebPurchaseListener.Result.InvalidToken -> {
                        showError = "Invalid web redemption token"
                    }
                    RedeemWebPurchaseListener.Result.PurchaseBelongsToOtherUser -> {
                        showError = "Web purchase belongs to a different user"
                    }
                    is RedeemWebPurchaseListener.Result.Expired -> {
                        showError = "Token expired. Email sent to ${result.obfuscatedEmail}"
                    }
                }
            }
            consumeCallback()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
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
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { showDetails = !showDetails },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CustomerInfo", style = MaterialTheme.typography.titleLarge)

                        customerInfo?.let { info ->
                            Text("Request Date: ${info.requestDate}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("User ID: ${info.originalAppUserId}", style = MaterialTheme.typography.bodyMedium)
                            Text("Verification: ${verificationResult.name}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Active Entitlements:", style = MaterialTheme.typography.labelLarge)
                            Text(activeEntitlements.ifBlank { "None" })
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All Entitlements:", style = MaterialTheme.typography.labelLarge)
                            Text(allEntitlements.ifBlank { "None" })

                            if (showDetails) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("JSON:", style = MaterialTheme.typography.labelLarge)
                                Text(info.rawData.toString(2), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { viewModel.onRestoreClicked() }, enabled = !isRestoring) {
                                Text("Restore")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (Purchases.sharedInstance.isAnonymous) {
                                    onNavigateToLogin()
                                } else {
                                    Purchases.sharedInstance.logOutWith(
                                        { error -> showError = error.message },
                                        { onNavigateToLogin() },
                                    )
                                }
                            }) {
                                Text("Logout")
                            }
                        }

                        customerInfo?.managementURL?.let { url ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, url)
                                context.startActivity(intent)
                            }) {
                                Text("Manage")
                            }
                        }
                    }
                }
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Offerings", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            offerings?.let { offeringsData ->
                val sortedOfferings = offeringsData.all.values.sortedBy {
                    it.identifier != offeringsData.current?.identifier
                }

                items(sortedOfferings) { offering ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onNavigateToOffering(offering.identifier) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val nameText = if (offering == offeringsData.current) {
                                "${offering.serverDescription} (current)"
                            } else {
                                offering.serverDescription
                            }
                            Text(nameText, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${offering.identifier}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${offering.availablePackages.size} packages",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Info") },
            text = { Text(showError!!) },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            },
        )
    }
}
