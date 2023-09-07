package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import java.util.Locale

@Composable
internal fun InternalPaywallView(
    offering: Offering? = null,
    viewModel: PaywallViewModel = viewModel<PaywallViewModelImpl>(factory = PaywallViewModelFactory(offering)),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = viewModel.state.collectAsState().value) {
            is PaywallViewState.Loading -> {
                Text(text = "Loading...")
            }
            is PaywallViewState.Error -> {
                Text(text = "Error: ${state.errorMessage}")
            }
            is PaywallViewState.Loaded -> {
                val offering = state.offering
                Text(text = "Paywall for offeringId: ${offering.identifier}")
                offering.paywall?.configForLocale(Locale.ENGLISH)?.let { localizedConfiguration ->
                    Text(
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center,
                        text = localizedConfiguration.title,
                    )
                    Text(
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center,
                        text = localizedConfiguration.subtitle ?: "",
                    )
                }
                val activity = LocalContext.current.getActivity() ?: error("Error finding activity")
                
                offering.availablePackages.forEach { aPackage ->
                    Button(onClick = { viewModel.purchasePackage(activity, aPackage) }) {
                        Text(text = "Purchase ${aPackage.identifier}. Price: ${aPackage.product.price.formatted}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

internal fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
