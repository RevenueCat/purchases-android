package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.widget.FrameLayout
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
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener
import com.revenuecat.purchases.ui.revenuecatui.fragments.PaywallFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun OfferingsScreen(
    tappedOnOffering: (Offering) -> Unit,
    viewModel: OfferingsViewModel = viewModel<OfferingsViewModelImpl>(),
) {
    when (val state = viewModel.offeringsState.collectAsState().value) {
        is OfferingsState.Error -> ErrorOfferingsScreen(errorState = state)
        is OfferingsState.Loaded -> OfferingsListScreen(
            offeringsState = state,
            tappedOnNavigateToOffering = tappedOnOffering,
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
) {
    var dropdownExpandedOffering by remember { mutableStateOf<Offering?>(null) }
    var displayPaywallDialogOffering by remember { mutableStateOf<Offering?>(null) }
    var displayPaywallFragmentOffering by remember { mutableStateOf<Offering?>(null) }

    LazyColumn {
        items(offeringsState.offerings.all.values.toList()) { offering ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (offering == dropdownExpandedOffering) {
                    DisplayOfferingMenu(
                        offering = offering,
                        tappedOnNavigateToOffering = tappedOnNavigateToOffering,
                        tappedOnDisplayOfferingAsDialog = { displayPaywallDialogOffering = it },
                        tappedOnDisplayOfferingAsFragment = { displayPaywallFragmentOffering = it },
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
                        Text(text = offering.identifier)
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
                .setListener(object : PaywallViewListener {
                    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                        displayPaywallDialogOffering = null
                    }
                })
                .build(),
        )
    }

    displayPaywallFragmentOffering?.let {
        PaywallFragmentComponent(it)
    }
}

@Composable
fun PaywallFragmentComponent(
    offering: Offering,
    modifier: Modifier = Modifier,
) {
    val fragmentManager = (LocalContext.current as FragmentActivity).supportFragmentManager
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                id = ViewCompat.generateViewId()
            }
        },
        update = {
            val tag = PaywallFragment.TAG
            val fragmentAlreadyAdded = fragmentManager.findFragmentByTag(tag) != null

            if (fragmentAlreadyAdded) {
                fragmentManager.beginTransaction()
                    .replace(it.id, PaywallFragment.newInstance(offering.identifier), tag)
                    .commit()
            } else {
                fragmentManager.beginTransaction()
                    .add(it.id, PaywallFragment.newInstance(offering.identifier), tag)
                    .commit()
            }
        },
    )
}

@Composable
private fun DisplayOfferingMenu(
    offering: Offering,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnDisplayOfferingAsDialog: (Offering) -> Unit,
    tappedOnDisplayOfferingAsFragment: (Offering) -> Unit,
    dismissed: () -> Unit,
) {
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
            text = { Text(text = "Display paywall as fragment") },
            onClick = { tappedOnDisplayOfferingAsFragment(offering) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OfferingsScreenPreview() {
    OfferingsScreen(
        tappedOnOffering = {},
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
