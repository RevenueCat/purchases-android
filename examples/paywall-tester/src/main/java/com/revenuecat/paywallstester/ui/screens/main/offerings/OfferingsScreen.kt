package com.revenuecat.paywallstester.ui.screens.main.offerings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.MainActivity
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun OfferingsScreen(
    tappedOnOffering: (Offering) -> Unit,
    tappedOnOfferingFooter: (Offering) -> Unit,
    tappedOnOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnOfferingByPlacement: (String) -> Unit,
    viewModel: OfferingsViewModel = viewModel<OfferingsViewModelImpl>(),
) {
    when (val state = viewModel.offeringsState.collectAsState().value) {
        is OfferingsState.Error -> ErrorOfferingsScreen(errorState = state)
        is OfferingsState.Loaded -> OfferingsListScreen(
            offeringsState = state,
            tappedOnNavigateToOffering = tappedOnOffering,
            tappedOnNavigateToOfferingFooter = tappedOnOfferingFooter,
            tappedOnNavigateToOfferingCondensedFooter = tappedOnOfferingCondensedFooter,
            tappedOnNavigateToOfferingByPlacement = tappedOnOfferingByPlacement,
        )
        OfferingsState.Loading -> LoadingOfferingsScreen()
    }
}

@Composable
private fun ErrorOfferingsScreen(errorState: OfferingsState.Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = errorState.purchasesError.toString())
    }
}

@Composable
private fun LoadingOfferingsScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Loading...")
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
private fun OfferingsListScreen(
    offeringsState: OfferingsState.Loaded,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnNavigateToOfferingFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingByPlacement: (String) -> Unit,
) {
    var dropdownExpandedOffering by remember { mutableStateOf<Offering?>(null) }
    var displayPaywallDialogOffering by remember { mutableStateOf<Offering?>(null) }

    val showDialog = remember { mutableStateOf(false) }
    val placementIdentifier = remember { mutableStateOf("") }
    val placementOffering = remember { mutableStateOf<Offering?>(null) }
    val noPlacementFoundMessage = remember { mutableStateOf<String?>(null) }

    LazyColumn {
        item {
            placementOffering.value?.let {
                DisplayOfferingMenu(
                    offering = it,
                    tappedOnNavigateToOffering = tappedOnNavigateToOffering,
                    tappedOnDisplayOfferingAsDialog = { displayPaywallDialogOffering = it },
                    tappedOnDisplayOfferingAsFooter = tappedOnNavigateToOfferingFooter,
                    tappedOnDisplayOfferingAsCondensedFooter = tappedOnNavigateToOfferingCondensedFooter,
                    dismissed = { dropdownExpandedOffering = null },
                )
            }
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
        items(offeringsState.offerings.all.values.toList()) { offering ->
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

                            offering.paywall?.let {
                                Text("Template ${it.templateName}")
                            } ?: run {
                                Text("No paywall")
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (displayPaywallDialogOffering != null) {
        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setDismissRequest { displayPaywallDialogOffering = null }
                .setOffering(displayPaywallDialogOffering)
                .build(),
        )
    }

    if (showDialog.value) {
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
//                                    placementOffering.value = offering
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
            onClick = { activity.launchPaywall(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as view in an activity") },
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
                        null,
                    ),
                ),
            )

            override val offeringsState: StateFlow<OfferingsState>
                get() = _offeringsState.asStateFlow()
        },
    )
}
