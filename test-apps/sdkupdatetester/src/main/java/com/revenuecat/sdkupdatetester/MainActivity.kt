/*
 * Created by Antonio Pallares on 3/10/26.
 * Copyright (c) 2026 RevenueCat, Inc. All rights reserved.
 */

@file:OptIn(ExperimentalComposeUiApi::class)

package com.revenuecat.sdkupdatetester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Purchases.logLevel = LogLevel.VERBOSE
        if (!Purchases.isConfigured) {
            Purchases.configure(PurchasesConfiguration.Builder(applicationContext, BuildConfig.API_KEY).build())
        }
        val appUserIdToLogIn = intent.getStringExtra("app_user_id_to_log_in")?.takeIf { it.isNotEmpty() }
        setContent {
            MaterialTheme {
                var showPurchaseScreen by remember { mutableStateOf(false) }
                BackHandler(enabled = showPurchaseScreen) { showPurchaseScreen = false }
                Surface(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    Column(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
                    ) {
                        Text(
                            text = if (showPurchaseScreen) "Purchase" else "SDK Update Tester",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        if (showPurchaseScreen) {
                            PurchaseScreen(activity = this@MainActivity)
                            OutlinedButton(onClick = { showPurchaseScreen = false }) { Text("Back") }
                        } else {
                            HomeScreen(
                                appUserIdToLogIn = appUserIdToLogIn,
                                onPurchaseScreen = { showPurchaseScreen = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
