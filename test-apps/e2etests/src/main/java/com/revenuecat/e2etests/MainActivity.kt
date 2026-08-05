package com.revenuecat.e2etests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.e2etests.main.MainPage
import com.revenuecat.e2etests.main.NoPaywallScreen
import com.revenuecat.e2etests.ui.theme.PurchasesandroidTheme
import com.revenuecat.e2etests.workflow.PresentedWorkflowScreen
import com.revenuecat.e2etests.workflow.WorkflowScreen
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler

class MainActivity : ComponentActivity(), PaywallResultHandler {

    private lateinit var paywallLauncher: PaywallActivityLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Registered before the activity is started, per ActivityResultLauncher rules. Used by the
        // presented-workflow flow to exercise the exit-offer-aware Activity path.
        paywallLauncher = PaywallActivityLauncher(this, this)

        // Maestro delivers launch arguments as Activity intent extras; here we only use them to
        // pick which screen to show and to pass a custom-variable override. Purchases is configured
        // in E2ETestsApplication.
        val flow = E2ETestFlow.fromRawValue(intent.getStringExtra(E2ETestFlow.INTENT_EXTRA_KEY))
        val usersCountOverride = intent.getStringExtra(CUSTOM_USERS_COUNT_EXTRA_KEY)?.toIntOrNull()
        setContent {
            PurchasesandroidTheme {
                when (flow) {
                    E2ETestFlow.OPEN_WORKFLOW -> WorkflowScreen(usersCountOverride = usersCountOverride)
                    E2ETestFlow.OPEN_WORKFLOW_PRESENTED -> PresentedWorkflowScreen(
                        onPresentPaywall = { paywallLauncher.launch(it) },
                    )
                    E2ETestFlow.OPEN_NO_PAYWALL -> NoPaywallScreen()
                    null -> MainPage()
                }
            }
        }
    }

    override fun onActivityResult(result: PaywallResult) {
        // No-op: dismissing the presented paywall returns here and shows the launcher screen again.
    }

    private companion object {
        const val CUSTOM_USERS_COUNT_EXTRA_KEY = "custom_users_count"
    }
}
