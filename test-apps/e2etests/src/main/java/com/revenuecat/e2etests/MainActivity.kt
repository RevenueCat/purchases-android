package com.revenuecat.e2etests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.e2etests.main.MainPage
import com.revenuecat.e2etests.main.NoPaywallScreen
import com.revenuecat.e2etests.ui.theme.PurchasesandroidTheme
import com.revenuecat.e2etests.workflow.WorkflowScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Maestro delivers launch arguments as Activity intent extras; here we only use them to
        // pick which screen to show and to pass a custom-variable override. Purchases is configured
        // in E2ETestsApplication.
        val flow = E2ETestFlow.fromRawValue(intent.getStringExtra(E2ETestFlow.INTENT_EXTRA_KEY))
        val usersCountOverride = intent.getStringExtra(CUSTOM_USERS_COUNT_EXTRA_KEY)?.toIntOrNull()
        setContent {
            PurchasesandroidTheme {
                when (flow) {
                    E2ETestFlow.OPEN_WORKFLOW -> WorkflowScreen(usersCountOverride = usersCountOverride)
                    E2ETestFlow.OPEN_WORKFLOW_PRESENTED -> WorkflowScreen()
                    E2ETestFlow.OPEN_NO_PAYWALL -> NoPaywallScreen()
                    null -> MainPage()
                }
            }
        }
    }

    private companion object {
        const val CUSTOM_USERS_COUNT_EXTRA_KEY = "custom_users_count"
    }
}
