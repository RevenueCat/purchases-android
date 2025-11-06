package com.revenuecat.purchasetester.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun OfferingScreen(
    offeringId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var offering by remember { mutableStateOf<Offering?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(offeringId) {
        Purchases.sharedInstance.getOfferingsWith(
            { error -> showError = error.message },
            { offerings ->
                offering = offerings.getOffering(offeringId)
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(offering?.identifier ?: "Offering") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        offering?.let { offeringData ->
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
                            .padding(vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                offeringData.identifier,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "Description: ${offeringData.serverDescription}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Packages", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(offeringData.availablePackages) { pkg ->
                    PackageCard(
                        pkg = pkg,
                        isLoading = isLoading,
                        onPurchase = { selectedPkg ->
                            isLoading = true
                            val params = com.revenuecat.purchases.PurchaseParams.Builder(
                                context as android.app.Activity,
                                selectedPkg,
                            ).build()
                            Purchases.sharedInstance.purchase(
                                params,
                                object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                                    override fun onCompleted(
                                        storeTransaction: StoreTransaction,
                                        customerInfo: CustomerInfo,
                                    ) {
                                        isLoading = false
                                        showError = "Purchase successful!"
                                    }

                                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                        isLoading = false
                                        if (!userCancelled) {
                                            showError = error.message
                                        }
                                    }
                                },
                            )
                        },
                    )
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

@Composable
fun PackageCard(
    pkg: Package,
    isLoading: Boolean,
    onPurchase: (Package) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                pkg.product.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                pkg.product.description,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Price: ${pkg.product.price.formatted}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Package Type: ${pkg.packageType}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onPurchase(pkg) },
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Buy")
            }
        }
    }
}
