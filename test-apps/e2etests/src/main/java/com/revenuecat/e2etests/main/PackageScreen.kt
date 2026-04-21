package com.revenuecat.e2etests.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import kotlinx.coroutines.launch

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageScreen(
    rcPackage: Package,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Package: ${rcPackage.identifier}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Package Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        InfoRow("Package ID", rcPackage.identifier)
                        InfoRow("Display Name", rcPackage.product.name)
                        InfoRow("Product ID", rcPackage.product.id)
                        InfoRow("Product Title", rcPackage.product.title)
                        InfoRow("Product Type", rcPackage.product.type.name)
                        InfoRow("Price", rcPackage.product.price.formatted)

                        // Purchase package button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        activity?.let {
                                            Purchases.sharedInstance.awaitPurchase(
                                                PurchaseParams.Builder(
                                                    activity = it,
                                                    packageToPurchase = rcPackage,
                                                ).build(),
                                            )
                                            snackbarHostState.showSnackbar("Purchase successful!")
                                            onBackClick()
                                        }
                                    } catch (e: PurchasesException) {
                                        snackbarHostState.showSnackbar("Purchase failed: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Purchase Package")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        // Purchase product button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        activity?.let {
                                            Purchases.sharedInstance.awaitPurchase(
                                                PurchaseParams.Builder(
                                                    activity = it,
                                                    storeProduct = rcPackage.product,
                                                ).build(),
                                            )
                                            snackbarHostState.showSnackbar("Purchase successful!")
                                            onBackClick()
                                        }
                                    } catch (e: PurchasesException) {
                                        snackbarHostState.showSnackbar("Purchase failed: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Purchase Product Directly")
                        }
                    }
                }
            }

            // Subscription options
            val subscriptionOptions = rcPackage.product.subscriptionOptions
            if (subscriptionOptions != null && subscriptionOptions.isNotEmpty()) {
                item {
                    Text(
                        text = "Subscription Options (${subscriptionOptions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(subscriptionOptions.size) { index ->
                    val option = subscriptionOptions[index]
                    SubscriptionOptionCard(
                        option = option,
                        onPurchase = {
                            scope.launch {
                                try {
                                    activity?.let {
                                        Purchases.sharedInstance.awaitPurchase(
                                            PurchaseParams.Builder(activity = it, subscriptionOption = option).build(),
                                        )
                                        snackbarHostState.showSnackbar("Purchase successful!")
                                        onBackClick()
                                    }
                                } catch (e: PurchasesException) {
                                    snackbarHostState.showSnackbar("Purchase failed: ${e.message}")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionOptionCard(
    option: SubscriptionOption,
    onPurchase: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Option: ${option.id}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            InfoRow("Option ID", option.id)
            option.presentedOfferingContext?.let {
                InfoRow("Offering ID", it.offeringIdentifier)
                InfoRow("Placement ID", it.placementIdentifier ?: "None")
                InfoRow("Targeting context", it.targetingContext?.toString() ?: "None")
            }

            // Pricing phases
            if (option.pricingPhases.isNotEmpty()) {
                Text(
                    text = "Pricing Phases",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )

                option.pricingPhases.forEachIndexed { index, phase ->
                    PricingPhaseRow(phase, index + 1)
                }
            }

            OutlinedButton(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Purchase This Option")
            }
        }
    }
}

@Composable
private fun PricingPhaseRow(phase: PricingPhase, phaseNumber: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp),
    ) {
        Text(
            text = "Phase $phaseNumber",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        InfoRow("Price", phase.price.formatted, small = true)
        InfoRow("Billing Period", phase.billingPeriod.iso8601, small = true)
        InfoRow("Billing Cycles", phase.billingCycleCount?.toString() ?: "Infinite", small = true)
        InfoRow("Recurrence Mode", phase.recurrenceMode.name, small = true)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    small: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}
