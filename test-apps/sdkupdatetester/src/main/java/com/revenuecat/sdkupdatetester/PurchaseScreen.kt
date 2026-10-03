/*
 * Created by Antonio Pallares on 3/10/26.
 * Copyright (c) 2026 RevenueCat, Inc. All rights reserved.
 */

package com.revenuecat.sdkupdatetester

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import kotlinx.coroutines.launch

private const val OFFERING_IDENTIFIER = "no_paywall"

@Composable
internal fun PurchaseScreen(activity: Activity) {
    var activeEntitlements by remember { mutableStateOf("Loading...") }
    var isPurchasing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val purchases = Purchases.sharedInstance

    LaunchedEffect(purchases) {
        try {
            activeEntitlements = purchases.awaitCustomerInfo().activeEntitlementsText()
        } catch (e: PurchasesException) {
            activeEntitlements = "Error: ${e.message}"
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
    ) {
        ActiveEntitlementsLabel(activeEntitlements)
        Button(
            onClick = {
                isPurchasing = true
                statusMessage = null
                scope.launch {
                    try {
                        statusMessage = purchaseMonthlySubscription(activity, purchases) {
                            activeEntitlements = it.activeEntitlementsText()
                        }
                    } catch (e: PurchasesTransactionException) {
                        statusMessage = if (e.userCancelled) "Purchase cancelled" else "Purchase failed: ${e.message}"
                    } catch (e: PurchasesException) {
                        statusMessage = "Purchase failed: ${e.message}"
                    } finally {
                        isPurchasing = false
                    }
                }
            },
            enabled = !isPurchasing,
            modifier = Modifier.testTag("purchase_button"),
        ) { Text("Purchase subscription") }
        statusMessage?.let {
            Text(it, textAlign = TextAlign.Center, modifier = Modifier.testTag("status_message"))
        }
    }
}

private fun CustomerInfo.activeEntitlementsText(): String =
    entitlements.active.keys.sorted().joinToString(", ").ifEmpty { "None" }

private suspend fun purchaseMonthlySubscription(
    activity: Activity,
    purchases: Purchases,
    onCustomerInfo: (CustomerInfo) -> Unit,
): String {
    val monthlyPackage = purchases.awaitOfferings().getOffering(OFFERING_IDENTIFIER)?.monthly
        ?: return "No monthly package found in the '$OFFERING_IDENTIFIER' offering"
    val result = purchases.awaitPurchase(PurchaseParams.Builder(activity, monthlyPackage).build())
    onCustomerInfo(result.customerInfo)
    return "Purchased ${monthlyPackage.product.id}"
}

@Composable
private fun ActiveEntitlementsLabel(activeEntitlements: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Active entitlements", style = MaterialTheme.typography.titleMedium)
        Text(
            text = activeEntitlements,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp).testTag("active_entitlements"),
        )
    }
}
