package com.revenuecat.sample.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.sample.utils.buttonText
import com.revenuecat.sample.utils.findActivity

@Suppress("LongParameterList")
@Composable
fun PaywallView(
    offering: Offering,
    modifier: Modifier = Modifier,
    onPurchaseStarted: ((Package) -> Unit)? = null,
    onPurchaseCompleted: ((CustomerInfo) -> Unit)? = null,
    onPurchaseCancelled: (() -> Unit)? = null,
    onPurchaseErrored: ((PurchasesError) -> Unit)? = null,
    marketingContent: (@Composable () -> Unit)? = null,
) {
    val viewModel: PaywallViewModel = viewModel()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (marketingContent == null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                DefaultMarketingContent()
            }
        } else {
            marketingContent()
        }
        Spacer(modifier = modifier.padding(8.dp))
        offering.availablePackages.forEach { packageToDisplay ->
            PackageButton(packageToDisplay, modifier) { activity, packageToPurchase ->
                viewModel.purchasePackage(
                    activity,
                    packageToPurchase,
                    onPurchaseStarted,
                    onPurchaseCompleted,
                    onPurchaseCancelled,
                    onPurchaseErrored,
                )
            }
        }
        Spacer(modifier = modifier.padding(16.dp))
    }
}

@Composable
private fun DefaultMarketingContent(modifier: Modifier = Modifier) {
    Text(
        text = "Upgrade to Premium and change the weather whenever you want!",
        modifier = modifier,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PackageButton(
    rcPackage: Package,
    modifier: Modifier = Modifier,
    onPurchaseClicked: (Activity, Package) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    Button(
        onClick = { onPurchaseClicked(context.findActivity(), rcPackage) },
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 32.dp,
                vertical = 8.dp,
            ),
    ) {
        Text(text = rcPackage.buttonText, color = Color.White)
    }
}
