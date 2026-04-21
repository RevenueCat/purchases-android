package com.revenuecat.e2etests.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("LongMethod")
@Composable
fun CustomerInfoPage(
    modifier: Modifier = Modifier,
) {
    var customerInfo by remember { mutableStateOf<CustomerInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        error = null
        try {
            customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        } catch (e: PurchasesException) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { refreshTrigger++ },
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                customerInfo != null -> {
                    CustomerInfoContent(customerInfo!!)
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun CustomerInfoContent(customerInfo: CustomerInfo) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            InfoSection(title = "Basic Info") {
                InfoItem("Original App User ID", customerInfo.originalAppUserId)
                InfoItem("First Seen", dateFormat.format(customerInfo.firstSeen))
                InfoItem("Request Date", dateFormat.format(customerInfo.requestDate))
                customerInfo.originalPurchaseDate?.let {
                    InfoItem("Original Purchase Date", dateFormat.format(it))
                }
                customerInfo.managementURL?.let {
                    InfoItem("Management URL", it.toString())
                }
            }
        }

        item {
            InfoSection(title = "Active Subscriptions") {
                if (customerInfo.activeSubscriptions.isEmpty()) {
                    InfoItem("Active Subscriptions", "None")
                } else {
                    customerInfo.activeSubscriptions.forEach { subscription ->
                        InfoItem("Subscription", subscription)
                    }
                }
            }
        }

        item {
            InfoSection(title = "All Purchased Product IDs") {
                if (customerInfo.allPurchasedProductIds.isEmpty()) {
                    InfoItem("Products", "None")
                } else {
                    customerInfo.allPurchasedProductIds.forEach { productId ->
                        InfoItem("Product ID", productId)
                    }
                }
            }
        }

        item {
            InfoSection(title = "Entitlements") {
                if (customerInfo.entitlements.all.isEmpty()) {
                    InfoItem("Entitlements", "None")
                } else {
                    customerInfo.entitlements.all.forEach { (key, entitlement) ->
                        InfoItem(
                            label = key,
                            value = "Active: ${entitlement.isActive}, Product: ${entitlement.productIdentifier}",
                        )
                    }
                }
            }
        }

        item {
            InfoSection(title = "Expiration Dates") {
                if (customerInfo.allExpirationDatesByProduct.isEmpty()) {
                    InfoItem("Expiration Dates", "None")
                } else {
                    customerInfo.allExpirationDatesByProduct.forEach { (productId, date) ->
                        InfoItem(
                            label = productId,
                            value = date?.let { dateFormat.format(it) } ?: "No expiration",
                        )
                    }
                }
            }
        }

        customerInfo.latestExpirationDate?.let { latestDate ->
            item {
                InfoSection(title = "Latest Expiration") {
                    InfoItem("Latest Expiration Date", dateFormat.format(latestDate))
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
