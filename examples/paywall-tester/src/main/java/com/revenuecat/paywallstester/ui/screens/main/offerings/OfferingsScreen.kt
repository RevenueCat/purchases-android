package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.MainActivity
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesEditorDialog
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesHolder
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.revenuecat.purchases.Package as RCPackage

@SuppressWarnings("LongParameterList")
@Composable
fun OfferingsScreen(
    tappedOnOffering: (Offering) -> Unit,
    tappedOnOfferingFooter: (Offering) -> Unit,
    tappedOnOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnOfferingByPlacement: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfferingsViewModel = viewModel<OfferingsViewModelImpl>(),
) {
    when (val state = viewModel.offeringsState.collectAsStateWithLifecycle().value) {
        is OfferingsState.Error -> ErrorOfferingsScreen(errorState = state, modifier)
        is OfferingsState.Loaded -> OfferingsListScreen(
            offeringsState = state,
            tappedOnNavigateToOffering = tappedOnOffering,
            tappedOnNavigateToOfferingFooter = tappedOnOfferingFooter,
            tappedOnNavigateToOfferingCondensedFooter = tappedOnOfferingCondensedFooter,
            tappedOnNavigateToOfferingByPlacement = tappedOnOfferingByPlacement,
            tappedOnReloadOfferings = { viewModel.refreshOfferings() },
            onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
            modifier,
        )
        OfferingsState.Loading -> LoadingOfferingsScreen(modifier)
    }
}

@Composable
private fun ErrorOfferingsScreen(
    errorState: OfferingsState.Error,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = errorState.purchasesError.toString())
    }
}

@Composable
private fun LoadingOfferingsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Loading...")
    }
}

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongMethod", "LongParameterList", "ViewModelInjection")
@Composable
private fun OfferingsListScreen(
    offeringsState: OfferingsState.Loaded,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnNavigateToOfferingFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingByPlacement: (String) -> Unit,
    tappedOnReloadOfferings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customVariablesViewModel: CustomVariablesViewModel = viewModel()
    var dropdownExpandedOffering by remember { mutableStateOf<Offering?>(null) }
    var displayPaywallDialogOffering by remember { mutableStateOf<Offering?>(null) }

    val showDialog = remember { mutableStateOf(false) }
    var showCustomVariablesEditor by remember { mutableStateOf(false) }

    // Filter offerings based on search query
    val filteredOfferings = remember(offeringsState.offerings, offeringsState.searchQuery) {
        val query = offeringsState.searchQuery.lowercase().trim()
        if (query.isEmpty()) {
            offeringsState.offerings.all.values.toList()
        } else {
            offeringsState.offerings.all.values.filter { offering ->
                offering.identifier.lowercase().contains(query) ||
                    offering.paywall?.templateName?.lowercase()?.contains(query) == true ||
                    offering.paywallComponents?.data?.templateName?.lowercase()?.contains(query) == true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            // Search bar
            item {
                OutlinedTextField(
                    value = offeringsState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = { Text("Search offerings...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    },
                    trailingIcon = {
                        if (offeringsState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showDialog.value = true }
                                .padding(16.dp),
                        ) {
                            Column {
                                Text("Get offering by placement")
                            }
                        }
                        Divider()
                    }
                }
            }
            items(filteredOfferings) { offering ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (offering == dropdownExpandedOffering) {
                        DisplayOfferingMenu(
                            offering = offering,
                            tappedOnNavigateToOffering = tappedOnNavigateToOffering,
                            tappedOnDisplayOfferingAsDialog = { displayPaywallDialogOffering = it },
                            tappedOnDisplayOfferingAsFooter = tappedOnNavigateToOfferingFooter,
                            tappedOnDisplayOfferingAsCondensedFooter = tappedOnNavigateToOfferingCondensedFooter,
                            dismissed = { dropdownExpandedOffering = null },
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpandedOffering = offering }
                                .padding(16.dp),
                        ) {
                            Column {
                                Text(text = offering.identifier)

                                offering.paywall?.also {
                                    Text("Template ${it.templateName}")
                                } ?: offering.paywallComponents?.also {
                                    Text("Components ${it.data.templateName}")
                                } ?: Text("No paywall")
                            }
                        }
                        Divider()
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingActionButton(
                onClick = { showCustomVariablesEditor = true },
            ) {
                Text(
                    text = "{ }",
                    fontWeight = FontWeight.Bold,
                )
            }
            FloatingActionButton(
                onClick = { tappedOnReloadOfferings() },
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh offerings",
                )
            }
        }
    }

    if (showCustomVariablesEditor) {
        CustomVariablesEditorDialog(
            viewModel = customVariablesViewModel,
            onDismiss = { showCustomVariablesEditor = false },
        )
    }

    if (displayPaywallDialogOffering != null) {
        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setDismissRequest { displayPaywallDialogOffering = null }
                .setOffering(displayPaywallDialogOffering)
                .setCustomVariables(CustomVariablesHolder.customVariables)
                .setListener(object : PaywallListener {
                    override fun onPurchaseStarted(rcPackage: RCPackage) {
                        Log.d("PaywallDialog", "onPurchaseStarted: ${rcPackage.identifier}")
                    }

                    override fun onPurchaseCompleted(
                        customerInfo: CustomerInfo,
                        storeTransaction: StoreTransaction,
                    ) {
                        Log.d("PaywallDialog", "onPurchaseCompleted: ${storeTransaction.productIds}")
                    }

                    override fun onPurchaseError(error: PurchasesError) {
                        Log.e("PaywallDialog", "onPurchaseError: ${error.message}")
                    }

                    override fun onRestoreStarted() {
                        Log.d("PaywallDialog", "onRestoreStarted")
                    }

                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        Log.d("PaywallDialog", "onRestoreCompleted: ${customerInfo.activeSubscriptions}")
                    }

                    override fun onRestoreError(error: PurchasesError) {
                        Log.e("PaywallDialog", "onRestoreError: ${error.message}")
                    }
                })
                .build(),
        )
    }

    if (showDialog.value) {
        PlacementDialog(tappedOnNavigateToOfferingByPlacement = tappedOnNavigateToOfferingByPlacement)
    }
}

@Composable
private fun PlacementDialog(
    tappedOnNavigateToOfferingByPlacement: (String) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    val placementIdentifier = remember { mutableStateOf("") }
    val noPlacementFoundMessage = remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            showDialog.value = false
            noPlacementFoundMessage.value = null
            placementIdentifier.value = ""
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Please enter text and submit")

                OutlinedTextField(
                    value = placementIdentifier.value,
                    onValueChange = { placementIdentifier.value = it },
                    label = { Text("Enter placement identifier") },
                )

                noPlacementFoundMessage.value?.let {
                    Text(it)
                }

                // Submit Button
                Button(
                    onClick = {
                        val placementId = placementIdentifier.value

                        Purchases.sharedInstance.getOfferingsWith {
                            it.getCurrentOfferingForPlacement(placementId)?.let { offering ->
                                showDialog.value = false
                                tappedOnNavigateToOfferingByPlacement(placementId)
                            } ?: run {
                                noPlacementFoundMessage.value = "No offering found for placement '$placementId'"
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DisplayOfferingMenu(
    offering: Offering,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnDisplayOfferingAsDialog: (Offering) -> Unit,
    tappedOnDisplayOfferingAsFooter: (Offering) -> Unit,
    tappedOnDisplayOfferingAsCondensedFooter: (Offering) -> Unit,
    dismissed: () -> Unit,
) {
    val activity = LocalContext.current as MainActivity
    DropdownMenu(expanded = true, onDismissRequest = { dismissed() }) {
        DropdownMenuItem(
            text = { Text(text = "Navigate to paywall") },
            onClick = { tappedOnNavigateToOffering(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as dialog") },
            onClick = { tappedOnDisplayOfferingAsDialog(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as footer") },
            onClick = { tappedOnDisplayOfferingAsFooter(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as condensed footer") },
            onClick = { tappedOnDisplayOfferingAsCondensedFooter(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as activity") },
            onClick = { activity.launchPaywall(offering, edgeToEdge = false) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as activity (edgeToEdge enabled)") },
            onClick = { activity.launchPaywall(offering, edgeToEdge = true) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as view in an activity (Purchase button gating example)") },
            onClick = { activity.launchPaywallViewAsActivity(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as footer view in an activity") },
            onClick = { activity.launchPaywallFooterViewAsActivity(offering) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OfferingsScreenPreview() {
    OfferingsScreen(
        tappedOnOffering = {},
        tappedOnOfferingFooter = {},
        tappedOnOfferingCondensedFooter = {},
        tappedOnOfferingByPlacement = {},
        viewModel = object : OfferingsViewModel() {
            private val _offeringsState = MutableStateFlow<OfferingsState>(
                OfferingsState.Loaded(
                    Offerings(
                        current = null,
                        all = emptyMap(),
                    ),
                ),
            )

            override val offeringsState: StateFlow<OfferingsState>
                get() = _offeringsState.asStateFlow()

            override fun refreshOfferings() {
                // no-op
            }

            override fun updateSearchQuery(query: String) {
                // no-op
            }
        },
    )
}
