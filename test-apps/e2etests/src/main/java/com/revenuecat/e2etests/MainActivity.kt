package com.revenuecat.e2etests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.e2etests.main.MainPage
import com.revenuecat.e2etests.ui.theme.PurchasesandroidTheme
import com.revenuecat.e2etests.workflow.WorkflowScreen
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val flow = E2ETestFlow.fromRawValue(intent.getStringExtra(E2ETestFlow.INTENT_EXTRA_KEY))
        configurePurchases(flow)

        enableEdgeToEdge()
        setContent {
            PurchasesandroidTheme {
                when (flow) {
                    E2ETestFlow.OPEN_WORKFLOW -> WorkflowScreen()
                    null -> MainPage()
                }
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun configurePurchases(flow: E2ETestFlow?) {
        if (Purchases.isConfigured) return

        val builder = when (flow) {
            E2ETestFlow.OPEN_WORKFLOW -> PurchasesConfiguration.Builder(
                context = this,
                apiKey = BuildConfig.WORKFLOWS_API_KEY,
            ).dangerousSettings(DangerousSettings.forWorkflows())

            null -> PurchasesConfiguration.Builder(
                context = this,
                apiKey = Constants.API_KEY,
            )
        }

        Purchases.configure(builder.build())
    }
}
