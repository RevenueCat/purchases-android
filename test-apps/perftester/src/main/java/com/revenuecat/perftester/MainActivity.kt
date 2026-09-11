package com.revenuecat.perftester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pressIndex = 0
    private var autorunHandled = false

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKey = BuildConfig.PERF_TESTER_API_KEY
        val keyMissing = apiKey.isBlank()
        val runner = PerfCycleRunner(applicationContext, apiKey)

        setContent {
            var workflowsEnabled by remember {
                mutableStateOf(intent.getStringExtra("workflows")?.toBoolean() ?: false)
            }
            var status by remember { mutableStateOf("Results: ${runner.resultsFilePath}") }
            var running by remember { mutableStateOf(false) }

            val runCycle = {
                if (!running) {
                    running = true
                    pressIndex += 1
                    status = "Running press #$pressIndex..."
                    lifecycleScope.launch {
                        val result = runner.runCycle(workflowsEnabled, pressIndex)
                        running = false
                        status = buildString {
                            appendLine("press #${result.pressIndexInProcess} workflows=${result.workflowsEnabled}")
                            appendLine("had_offerings_cache=${result.hadOfferingsCache}")
                            appendLine("configure_to_offerings_ms=${result.configureToOfferingsMs}")
                            appendLine("get_offerings_ms=${result.getOfferingsMs}")
                            appendLine("success=${result.success}")
                            result.errorMessage?.let { appendLine("error: $it") }
                            append("file: ${runner.resultsFilePath}")
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (!autorunHandled && !keyMissing && intent.getStringExtra("autorun")?.toBoolean() == true) {
                    autorunHandled = true
                    runCycle()
                }
            }

            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = workflowsEnabled,
                            onCheckedChange = { workflowsEnabled = it },
                            enabled = !running,
                        )
                        Text(if (workflowsEnabled) "workflows" else "baseline")
                    }
                    Button(onClick = runCycle, enabled = !running && !keyMissing) {
                        Text("Configure + getOfferings")
                    }
                    Button(
                        onClick = {
                            if (Purchases.isConfigured) {
                                Purchases.sharedInstance.close()
                            }
                            SdkCaches.clearAll(applicationContext)
                            status = "SDK caches cleared."
                        },
                        enabled = !running,
                    ) {
                        Text("Clear SDK caches")
                    }
                    if (keyMissing) {
                        Text("Set PERF_TESTER_API_KEY in local.properties and rebuild.")
                    }
                    Text(status)
                }
            }
        }
    }
}
