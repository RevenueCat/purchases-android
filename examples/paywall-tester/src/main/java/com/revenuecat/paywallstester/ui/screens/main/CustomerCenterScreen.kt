package com.revenuecat.paywallstester.ui.screens.main

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions

private const val TAG = "CustomerCenterTest"

@Composable
fun CustomerCenterScreen(
    dismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastCustomAction by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val customerCenterListener = remember {
        createCustomerCenterListener { actionIdentifier, purchaseIdentifier ->
            val message = "Custom Action: $actionIdentifier" +
                if (purchaseIdentifier != null) " (Product: $purchaseIdentifier)" else ""
            lastCustomAction = message
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    CustomerCenter(
        modifier = modifier.fillMaxSize(),
        options = CustomerCenterOptions.Builder()
            .setListener(customerCenterListener)
            .build(),
    ) {
        dismissRequest()
    }
}

internal fun createCustomerCenterListener(
    tag: String = TAG,
    onCustomAction: (actionIdentifier: String, purchaseIdentifier: String?) -> Unit = { _, _ -> },
): CustomerCenterListener {
    return object : CustomerCenterListener {
        override fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
            Log.d(tag, "Local listener: onManagementOptionSelected called with action: $action")
        }

        override fun onRestoreStarted() {
            Log.d(tag, "Local listener: onRestoreStarted called")
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            Log.d(
                tag,
                "Local listener: onRestoreCompleted called with customer info: " +
                    customerInfo.originalAppUserId,
            )
        }

        override fun onRestoreFailed(error: PurchasesError) {
            Log.d(tag, "Local listener: onRestoreFailed called with error: ${error.message}")
        }

        override fun onShowingManageSubscriptions() {
            Log.d(tag, "Local listener: onShowingManageSubscriptions called")
        }

        override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
            Log.d(tag, "Local listener: onFeedbackSurveyCompleted called with option ID: $feedbackSurveyOptionId")
        }

        override fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) {
            Log.d(
                tag,
                "Local listener: onCustomActionSelected called with action: $actionIdentifier, " +
                    "purchaseIdentifier: $purchaseIdentifier",
            )
            onCustomAction(actionIdentifier, purchaseIdentifier)
        }
    }
}
