package com.revenuecat.sample.admob.nextgen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

private const val DEFAULT_BUFFER_SIZE = 2
private const val MIN_BUFFER_SIZE = 1
private const val MAX_SAMPLE_BUFFER_SIZE = 5
private const val REFRESH_INTERVAL_MILLIS = 500L

@Stable
internal class PreloaderUiState(
    private val getConfiguration: () -> PreloadConfiguration?,
    private val getNumAdsAvailable: () -> Int,
) {
    var started by mutableStateOf(false)
        private set
    var bufferSize by mutableIntStateOf(DEFAULT_BUFFER_SIZE)
        private set
    var adsAvailable by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        val wasStarted = started
        val configuration = getConfiguration()
        started = configuration != null
        configuration?.bufferSize?.let { bufferSize = it }
        adsAvailable = if (started) getNumAdsAvailable() else 0

        if (wasStarted != started) {
            message = null
        }
    }

    fun updateBufferSize(newBufferSize: Int) {
        if (!started) bufferSize = newBufferSize.coerceIn(MIN_BUFFER_SIZE, MAX_SAMPLE_BUFFER_SIZE)
    }

    fun preloadCallback(scope: CoroutineScope): PreloadCallback = preloadStatusCallback(
        scope = scope,
        onAdPreloaded = {
            refresh()
            message = "Ad preloaded"
        },
        updateStatus = { status ->
            message = status
            refresh()
        },
    )

    fun toggle(start: () -> Boolean, stop: () -> Boolean) {
        if (started) {
            updateAfterStop(stop())
        } else {
            updateAfterStart(start())
        }
    }

    fun updateAfterStart(startResult: Boolean) {
        refresh()
        message = when {
            startResult -> "Preloader started; waiting for ads"
            started -> "Preloader already started"
            else -> "Preloader did not start"
        }
    }

    fun updateAfterStop(stopResult: Boolean) {
        refresh()
        message = if (stopResult) null else "Preloader was not running"
    }
}

@Composable
internal fun rememberPreloaderUiState(
    preloadId: String,
    getConfiguration: () -> PreloadConfiguration?,
    getNumAdsAvailable: () -> Int,
): PreloaderUiState = remember(preloadId) {
    PreloaderUiState(getConfiguration, getNumAdsAvailable)
}

internal data class PreloaderAction(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun PreloaderPanel(
    state: PreloaderUiState,
    actions: List<PreloaderAction> = emptyList(),
    additionalMetrics: @Composable () -> Unit = {},
    onToggle: () -> Unit,
) {
    LaunchedEffect(state) {
        while (true) {
            state.refresh()
            delay(REFRESH_INTERVAL_MILLIS)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreloaderHeader(state, onToggle)
            HorizontalDivider()
            BufferSizeSetting(state)
            MetricRow("Ads ready", state.adsAvailable.toString())
            additionalMetrics()
            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actions.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    actions.forEach { action ->
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = action.onClick,
                            enabled = action.enabled,
                        ) {
                            Text(action.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreloaderHeader(state: PreloaderUiState, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Preload buffer",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            shape = CircleShape,
            color = if (state.started) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Text(
                text = if (state.started) "Running" else "Stopped",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedButton(onClick = onToggle) {
            Text(if (state.started) "Stop" else "Start")
        }
    }
}

@Composable
private fun BufferSizeSetting(state: PreloaderUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NumericCountSetting(
            label = {
                Text(
                    text = "Buffer capacity",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            value = state.bufferSize,
            valueRange = MIN_BUFFER_SIZE..MAX_SAMPLE_BUFFER_SIZE,
            enabled = !state.started,
            onValueChange = state::updateBufferSize,
        )
    }
}

@Composable
internal fun NumericCountSetting(
    label: @Composable () -> Unit,
    value: Int,
    valueRange: IntRange,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) { label() }
        OutlinedButton(
            onClick = { onValueChange(value - 1) },
            enabled = enabled && value > valueRange.first,
        ) {
            Text("−")
        }
        Text(value.toString())
        OutlinedButton(
            onClick = { onValueChange(value + 1) },
            enabled = enabled && value < valueRange.last,
        ) {
            Text("+")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
