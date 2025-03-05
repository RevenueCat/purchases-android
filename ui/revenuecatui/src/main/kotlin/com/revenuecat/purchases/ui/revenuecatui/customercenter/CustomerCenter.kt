package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
* Use the Customer Center in your app to help your customers manage common support tasks.
*
* Customer Center is a self-service UI that can be added to your app to help
* your customers manage their subscriptions on their own. With it, you can prevent
* churn with pre-emptive promotional offers, capture actionable customer data with
* exit feedback prompts, and lower support volumes for common inquiries — all
* without any help from your support team.
*
* The `CustomerCenter` composable is a full screen UI that can be used to integrate
* the Customer Center directly in your app with Compose.
*
* For more information, see the [Customer Center docs](https://www.revenuecat.com/docs/tools/customer-center).
*/
@Composable
fun CustomerCenter(
    modifier: Modifier = Modifier,
    options: CustomerCenterOptions = CustomerCenterOptions.Builder().build(),
    onDismiss: () -> Unit,
) {
    InternalCustomerCenter(
        modifier = modifier,
        listener = options.listener,
        onDismiss = onDismiss,
    )
}
