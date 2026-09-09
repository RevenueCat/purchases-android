package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.previewWindowSizeConditionsState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Snapshots for window size conditions, sharing the preview fixture: two panes stacked
 * vertically by default, flipped side by side by an override conditioned on
 * `window width >= 700dp AND height >= 480dp`. One config per outcome: phone portrait
 * stays stacked (width), phone landscape stays stacked (the height floor), tablet flips.
 */
@RunWith(Parameterized::class)
internal class WindowSizeConditionsSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun splitLayoutFollowsWindowSize() {
        screenshotTest {
            LoadedPaywallComponents(
                state = previewWindowSizeConditionsState(),
                clickHandler = { },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = testConfigs
            .filter { it.name in setOf("pixel6", "pixel6_landscape", "nexus10") }
            .map { arrayOf<Any>(it) }
    }
}
