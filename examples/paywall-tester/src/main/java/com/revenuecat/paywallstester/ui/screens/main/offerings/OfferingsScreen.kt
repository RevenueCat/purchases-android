package com.revenuecat.paywallstester.ui.screens.main.offerings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
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
        is OfferingsState.Loaded -> OfferingsListScreen(offeringsState = state, tappedOnOffering = tappedOnOffering)
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
private fun OfferingsListScreen(offeringsState: OfferingsState.Loaded, tappedOnOffering: (Offering) -> Unit) {
    LazyColumn {
        items(offeringsState.offerings.all.values.toList()) { offering ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { tappedOnOffering(offering) }
                        .padding(16.dp),
                ) {
                    Text(text = offering.identifier)
                }
                Divider()
            }
        }
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
