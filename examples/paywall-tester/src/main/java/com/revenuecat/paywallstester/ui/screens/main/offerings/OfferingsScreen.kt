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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.MainActivity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun OfferingsScreen(
    tappedOnOffering: (Offering) -> Unit,
    tappedOnOfferingFooter: (Offering) -> Unit,
    viewModel: OfferingsViewModel = viewModel<OfferingsViewModelImpl>(),
) {
    when (val state = viewModel.offeringsState.collectAsState().value) {
        is OfferingsState.Error -> ErrorOfferingsScreen(errorState = state)
        is OfferingsState.Loaded -> OfferingsListScreen(
            offeringsState = state,
            tappedOnNavigateToOffering = tappedOnOffering,
            tappedOnNavigateToOfferingFooter = tappedOnOfferingFooter,
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

@Composable
private fun OfferingsListScreen(
    offeringsState: OfferingsState.Loaded,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnNavigateToOfferingFooter: (Offering) -> Unit,
) {
    var dropdownExpandedOffering by remember { mutableStateOf<Offering?>(null) }
    var displayPaywallDialogOffering by remember { mutableStateOf<Offering?>(null) }

    LazyColumn {
        items(offeringsState.offerings.all.values.toList()) { offering ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (offering == dropdownExpandedOffering) {
                    DisplayOfferingMenu(
                        offering = offering,
                        tappedOnNavigateToOffering = tappedOnNavigateToOffering,
                        tappedOnDisplayOfferingAsDialog = { displayPaywallDialogOffering = it },
                        tappedOnDisplayOfferingAsFooter = tappedOnNavigateToOfferingFooter,
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
            PaywallDialogOptions.Builder(
                dismissRequest = {
                    displayPaywallDialogOffering = null
                },
            )
                .setOffering(displayPaywallDialogOffering)
                .setListener(object : PaywallListener {
                    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                        displayPaywallDialogOffering = null
                    }
                })
                .build(),
        )
    }
}

@Composable
private fun DisplayOfferingMenu(
    offering: Offering,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnDisplayOfferingAsDialog: (Offering) -> Unit,
    tappedOnDisplayOfferingAsFooter: (Offering) -> Unit,
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
            text = { Text(text = "Display paywall as activity") },
            onClick = { activity.launchPaywall(offering) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OfferingsScreenPreview() {
    OfferingsScreen(
        tappedOnOffering = {},
        tappedOnOfferingFooter = {},
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
        },
    )
}
