package com.revenuecat.webpurchaseredemptionsample

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import kotlinx.coroutines.launch

@Composable
fun Content(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var customerInfo: CustomerInfo? by remember { mutableStateOf(null) }
    var isAnonymous by remember { mutableStateOf(Purchases.sharedInstance.isAnonymous) }
    LaunchedEffect(Unit) {
        try {
            customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        } catch (e: PurchasesException) {
            Log.e("WebPurchaseSample", "Error fetching customer info", e)
        }
    }
    var shouldShowLoginDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your user ID:\n${Purchases.sharedInstance.appUserID}", textAlign = TextAlign.Center)
        EntitlementInfo(customerInfo)
        Button(onClick = {
            if (Purchases.sharedInstance.isAnonymous) {
                shouldShowLoginDialog = true
            } else {
                coroutineScope.launch {
                    try {
                        customerInfo = Purchases.sharedInstance.awaitLogOut()
                        isAnonymous = true
                    } catch (e: PurchasesException) {
                        Log.e("WebPurchaseSample", "Error logging out", e)
                    }
                }
            }
        }) {
            if (isAnonymous) {
                Text("LogIn")
            } else {
                Text("LogOut")
            }
        }
    }
    if (shouldShowLoginDialog) {
        InputTextDialog(
            title = "Log In",
            message = "Please enter the new user ID",
            onDismiss = { shouldShowLoginDialog = false },
            onConfirm = { newUserId ->
                coroutineScope.launch {
                    try {
                        customerInfo = Purchases.sharedInstance.awaitLogIn(newUserId).customerInfo
                        shouldShowLoginDialog = false
                        isAnonymous = false
                    } catch (e: PurchasesException) {
                        Log.e("WebPurchaseSample", "Error logging out", e)
                    }
                }
            },
        )
    }
    WebRedemption { newCustomerInfo ->
        customerInfo = newCustomerInfo
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
fun WebRedemption(onSuccess: (CustomerInfo) -> Unit) {
    var dialogMessage: String? by remember { mutableStateOf(null) }
    val activity = (LocalContext.current as? MainActivity)
    val webPurchaseRedemption = activity?.webPurchaseRedemption
    if (webPurchaseRedemption != null) {
        LaunchedEffect(webPurchaseRedemption, onSuccess) {
            Purchases.sharedInstance.redeemWebPurchase(webPurchaseRedemption) { result ->
                dialogMessage = when (result) {
                    is RedeemWebPurchaseListener.Result.Success -> {
                        onSuccess(result.customerInfo)
                        "Successfully redeemed purchase!"
                    }
                    RedeemWebPurchaseListener.Result.AlreadyRedeemed ->
                        "Purchase already redeemed. Nothing to do."
                    is RedeemWebPurchaseListener.Result.Error ->
                        "Error redeeming purchase: ${result.error.message}"
                    is RedeemWebPurchaseListener.Result.Expired ->
                        "Link expired. New link has been sent to ${result.obfuscatedEmail}"
                    RedeemWebPurchaseListener.Result.InvalidToken ->
                        "Invalid token. Please try again."
                }
                activity.clearWebPurchaseRedemption()
            }
        }
    }
    val currentDialogMessage = dialogMessage
    if (currentDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Redeem Web Purchase result") },
            text = { Text(currentDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = { dialogMessage = null },
                ) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
fun EntitlementInfo(
    customerInfo: CustomerInfo?,
    modifier: Modifier = Modifier,
) {
    val hasEntitlement = customerInfo?.entitlements?.active?.containsKey(Constants.ENTITLEMENT_ID) == true
    val color = if (hasEntitlement) {
        Color.Green
    } else {
        Color.Red
    }
    val text = if (hasEntitlement) {
        "You have access to ${Constants.ENTITLEMENT_ID}!"
    } else {
        "You don't have access to ${Constants.ENTITLEMENT_ID} \uD83D\uDE1E"
    }
    @Suppress("MagicNumber")
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(color.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(text)
    }
}

@Composable
fun InputTextDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column {
                Text(text = message)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(text = "Enter text here") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text)
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}
