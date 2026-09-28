package com.revenuecat.purchases.ui.debugview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import com.android.ide.common.rendering.api.SessionParams
import com.revenuecat.snapshots.SentrySnapshotHandler
import com.revenuecat.snapshots.SentrySnapshotMetadata
import com.revenuecat.purchases.ui.debugview.models.SettingGroupState
import com.revenuecat.purchases.ui.debugview.models.SettingScreenState
import com.revenuecat.purchases.ui.debugview.models.SettingState
import com.revenuecat.purchases.ui.debugview.models.testOffering
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatViewModel
import com.revenuecat.purchases.ui.debugview.InternalDebugRevenueCatScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("TooManyFunctions")
class DebugViewSnapshotTest {

    private val maxHeight = 3000.dp

    @get:Rule
    val paparazzi = Paparazzi(
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        snapshotHandler = SentrySnapshotHandler(Snapshot::toSentryMetadata),
    )

    private lateinit var notConfiguredStateFlow: MutableStateFlow<SettingScreenState>
    private lateinit var notConfiguredViewModel: DebugRevenueCatViewModel
    private lateinit var configuredStateFlow: MutableStateFlow<SettingScreenState>
    private lateinit var configuredViewModel: DebugRevenueCatViewModel

    @Before
    fun setUp() {
        setupTestViewModels()
    }

    @Test
    fun notConfiguredStateDisplaysCorrectly() {
        snapshotTest {
            InternalDebugRevenueCatScreen(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                screenViewModel = notConfiguredViewModel,
                activity = mockk()
            )
        }
    }

    @Suppress("MagicNumber")
    @Test
    fun configuredStateDisplaysCorrectly() {
        snapshotTest {
            InternalDebugRevenueCatScreen(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                screenViewModel = configuredViewModel,
                activity = mockk(),
            )
        }
    }

    private fun snapshotTest(composable: @Composable () -> Unit) {
        paparazzi.snapshot {
            Column(modifier = Modifier.heightIn(max = maxHeight)) {
                composable()
            }
        }
    }

    private fun setupTestViewModels() {
        setupNotConfiguredViewModel()
        setupConfiguredViewModel()
    }

    private fun setupNotConfiguredViewModel() {
        notConfiguredStateFlow = MutableStateFlow(
            SettingScreenState.NotConfigured(
                SettingGroupState(
                    "Configuration",
                    listOf(
                        SettingState.Text("Title", "Value"),
                        SettingState.Text("Title2", "Value2"),
                    ),
                ),
            ),
        )
        notConfiguredViewModel = mockk<DebugRevenueCatViewModel>().apply {
            every { state } returns notConfiguredStateFlow
        }
    }

    private fun setupConfiguredViewModel() {
        configuredStateFlow = MutableStateFlow(
            SettingScreenState.Configured(
                SettingGroupState(
                    "Configuration",
                    listOf(
                        SettingState.Text("Title", "Value"),
                        SettingState.Text("Title2", "Value2"),
                    ),
                ),
                SettingGroupState(
                    "Customer Info",
                    listOf(
                        SettingState.Text("Title3", "Value3"),
                        SettingState.Text("Title4", "Value4"),
                    ),
                ),
                SettingGroupState(
                    "Offerings",
                    listOf(
                        SettingState.OfferingSetting(testOffering),
                        SettingState.OfferingSetting(testOffering),
                    ),
                ),
            ),
        )
        configuredViewModel = mockk<DebugRevenueCatViewModel>().apply {
            every { state } returns configuredStateFlow
        }
    }
}

private fun Snapshot.toSentryMetadata(): SentrySnapshotMetadata {
    val methodName = testName.methodName.substringBefore('[')
    return SentrySnapshotMetadata(
        displayName = methodName,
        group = "debugview/${testName.className}",
        tags = mapOf(
            "module" to "debugview",
            "test_class" to testName.className,
        ),
        context = buildJsonObject {
            put("test_name", "${testName.packageName}.${testName.className}#$methodName")
            putJsonObject("paparazzi") {
                put("package_name", testName.packageName)
                put("class_name", testName.className)
                put("method_name", methodName)
                name?.takeIf(String::isNotBlank)?.let { put("snapshot_name", it) }
            }
        },
    )
}
