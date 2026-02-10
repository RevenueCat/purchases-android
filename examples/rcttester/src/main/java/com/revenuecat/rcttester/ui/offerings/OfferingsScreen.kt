package com.revenuecat.rcttester.ui.offerings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offerings by remember { mutableStateOf<Offerings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedOfferingForMetadata by remember { mutableStateOf<Offering?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        error = null
        if (!Purchases.isConfigured) {
            error = "SDK not configured"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            offerings = Purchases.sharedInstance.awaitOfferings()
        } catch (e: PurchasesException) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offerings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Failed to load offerings",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = {
                            if (Purchases.isConfigured) {
                                refreshKey++
                            } else {
                                error = "SDK not configured"
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                offerings != null -> {
                    if (offerings!!.all.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "No Offerings",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No offerings are configured for this app.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Move current offering to top if it exists
                            val offeringsList = offerings!!.all.values.toList()
                            val currentOffering = offerings!!.current
                            val sortedOfferings = if (currentOffering != null && offeringsList.firstOrNull()?.identifier != currentOffering.identifier) {
                                listOf(currentOffering) + offeringsList.filter { it.identifier != currentOffering.identifier }
                            } else {
                                offeringsList
                            }

                            items(sortedOfferings) { offering ->
                                OfferingCard(
                                    offering = offering,
                                    isCurrent = offering.identifier == currentOffering?.identifier,
                                    onShowMetadata = { selectedOfferingForMetadata = offering },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Metadata Dialog
    selectedOfferingForMetadata?.let { offering ->
        OfferingMetadataDialog(
            offering = offering,
            onDismiss = { selectedOfferingForMetadata = null },
        )
    }
}

@Composable
private fun OfferingCard(
    offering: Offering,
    isCurrent: Boolean,
    onShowMetadata: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offering.identifier,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (offering.serverDescription.isNotEmpty()) {
                        Text(
                            text = offering.serverDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onShowMetadata) {
                    Icon(Icons.Default.Info, contentDescription = "Show metadata")
                }
            }

            if (isCurrent) {
                Text(
                    text = "CURRENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Packages
            if (offering.availablePackages.isNotEmpty()) {
                Text(
                    text = "Packages:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                offering.availablePackages.forEach { packageItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packageItem.product.title,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = packageItem.identifier,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = packageItem.product.price.formatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                Text(
                    text = "No packages available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OfferingMetadataDialog(
    offering: Offering,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offering Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    MetadataRow("Identifier", offering.identifier)
                }
                item {
                    MetadataRow("Description", offering.serverDescription)
                }
                item {
                    MetadataRow("Has Paywall", if (offering.webCheckoutURL != null) "Yes" else "No")
                }
                item {
                    Text(
                        text = "Packages (${offering.availablePackages.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(offering.availablePackages) { packageItem ->
                    Column(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    ) {
                        Text(
                            text = packageItem.product.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = packageItem.identifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = packageItem.product.price.formatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (offering.metadata.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Metadata",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(offering.metadata.keys.sorted()) { key ->
                        MetadataRow(key, offering.metadata[key]?.toString() ?: "null")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
