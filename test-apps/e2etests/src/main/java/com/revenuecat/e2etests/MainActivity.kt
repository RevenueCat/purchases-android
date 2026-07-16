package com.revenuecat.e2etests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.e2etests.main.MainPage
import com.revenuecat.e2etests.ui.theme.PurchasesandroidTheme
import com.revenuecat.e2etests.workflow.WorkflowScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Maestro delivers launch arguments as Activity intent extras; here we only use the flow to
        // pick which screen to show. Purchases is configured in E2ETestsApplication.
        val flow = E2ETestFlow.fromRawValue(intent.getStringExtra(E2ETestFlow.INTENT_EXTRA_KEY))
        setContent {
            PurchasesandroidTheme {
                when (flow) {
                    E2ETestFlow.OPEN_WORKFLOW -> WorkflowScreen()
                    null -> MainPage()
                }
            }
        }
    }
}
