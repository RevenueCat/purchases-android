@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("MatchingDeclarationName")

package com.revenuecat.sample.admob.nextgen.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.admob.nextgen.MainApplication
import com.revenuecat.sample.admob.nextgen.SampleScreen

internal enum class LoadMode(val label: String) {
    DIRECT("Direct"),
    PRELOADED("Preloaded"),
}

@Composable
fun AdMobNextGenSample(
    @Suppress("UnusedParameter")
    activity: Activity,
    application: MainApplication,
    selectedScreen: SampleScreen?,
    onSelectScreen: (SampleScreen) -> Unit,
    onBack: () -> Unit,
) {
    if (selectedScreen == null) {
        HomeScreen(application, onSelectScreen)
        return
    }

    when (selectedScreen) {
        SampleScreen.BANNER -> BannerScreen(activity, onBack)
        SampleScreen.INTERSTITIAL -> InterstitialScreen(activity, onBack)
        SampleScreen.APP_OPEN -> AppOpenScreen(activity, onBack)
        SampleScreen.REWARDED -> RewardedScreen(activity, onBack)
        SampleScreen.REWARDED_INTERSTITIAL -> RewardedInterstitialScreen(activity, onBack)
        SampleScreen.NATIVE -> NativeScreen(onBack)
        SampleScreen.DIAGNOSTICS -> DiagnosticsScreen(onBack)
    }
}

@Composable
private fun HomeScreen(application: MainApplication, onSelectScreen: (SampleScreen) -> Unit) {
    val adsStatus by application.adsStatus.collectAsState()
    val revenueCatStatus by application.revenueCatStatus.collectAsState()
    val sdksReady = adsStatus.ready && revenueCatStatus.ready
    SampleScaffold(title = "AdMob Next-Gen", contentPadding = PaddingValues(16.dp)) {
        Text(revenueCatStatus.message, style = MaterialTheme.typography.bodyMedium)
        Text(adsStatus.message, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Every format demonstrates RevenueCat tracking through both direct loading and Google's preloaders.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SampleScreen.entries.forEach { screen ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectScreen(screen) },
                enabled = sdksReady,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(screen.title)
                    Text(screen.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun AdScreen(
    title: String,
    onBack: () -> Unit,
    onModeChange: () -> Unit = {},
    content: @Composable (LoadMode) -> Unit,
) {
    var mode by remember { mutableStateOf(LoadMode.DIRECT) }
    SampleScaffold(title = title, onBack = onBack, contentPadding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = {
                        if (mode != option) {
                            mode = option
                            onModeChange()
                        }
                    },
                    label = { Text(option.label) },
                )
            }
        }
        content(mode)
    }
}

@Composable
internal fun StatusCard(status: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = status,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun ActionRow(
    vararg actions: Pair<String, () -> Unit>,
    enabled: Map<String, Boolean> = emptyMap(),
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { (label, action) ->
            Button(
                modifier = Modifier.weight(1f),
                onClick = action,
                enabled = enabled[label] ?: true,
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun SampleScaffold(
    title: String,
    contentPadding: PaddingValues,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
