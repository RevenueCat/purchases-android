package com.revenuecat.sample.paywall

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Package
import com.revenuecat.sample.utils.buttonText
import com.revenuecat.sample.utils.findActivity

@Suppress("LongParameterList")
@Composable
fun PaywallSuccessView(
    uiState: PaywallState.Success,
    modifier: Modifier = Modifier,
    purchaseListener: PurchaseListener? = null,
    marketingContent: (@Composable () -> Unit)? = null,
) {
    val viewModel: PaywallViewModel = viewModel()
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (marketingContent == null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                DefaultMarketingContent(state = uiState)
            }
        } else {
            marketingContent()
        }
        Spacer(modifier = modifier.padding(8.dp))
        uiState.offering.availablePackages
            .sortedBy { it.product.period?.unit }
            .forEach { packageToDisplay ->
                PackageButton(packageToDisplay, modifier) { activity, packageToPurchase ->
                    viewModel.purchasePackage(
                        activity,
                        packageToPurchase,
                        purchaseListener,
                    )
                }
            }
        Spacer(modifier = modifier.padding(16.dp))
    }
}

@Composable
private fun DefaultMarketingContent(
    modifier: Modifier = Modifier,
    state: PaywallState.Success,
) {
    Column {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.title,
                    modifier = modifier,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                )
                Text(
                    text = state.subtitle,
                    modifier = modifier,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle1,
                )
            }
        }
        Image(
            painter = painterResource(id = state.imageResource),
            modifier = modifier.clip(RoundedCornerShape(10.dp)).weight(2f, true),
            alignment = Alignment.Center,
            contentDescription = null,
        )
    }
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
                vertical = 8.dp,
            ),
    ) {
        Text(text = rcPackage.buttonText, color = Color.White)
    }
}
