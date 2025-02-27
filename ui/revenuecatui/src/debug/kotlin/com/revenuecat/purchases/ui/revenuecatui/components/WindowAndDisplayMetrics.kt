package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * This is just a helper to get visibility into, and track changes in, preview rendering environments we don't control,
 * such as Emerge Snapshots. If this preview breaks in a snapshot test, it probably means that the environment has
 * changed.
 */
@Suppress("LongMethod")
@Preview
@Composable
private fun WindowAndDisplayMetrics() {
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val displayMetrics = context.resources.displayMetrics
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val constraints = constraints
        Column {
            Text(
                text = "Window and display metrics",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "This is just a helper to get visibility into, and track changes in, preview rendering " +
                    "environments we don't control, such as Emerge Snapshots. If this preview breaks in a snapshot " +
                    "test, it probably means that the environment has changed.",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                text = "Density",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )

            Label(text = "Density.density")
            Text(text = "${density.density}, ${density.density * 160f} dpi")

            Label(text = "Configuration.densityDpi")
            Text(text = "${configuration.densityDpi / 160f}, ${configuration.densityDpi} dpi")

            Label(text = "DisplayMetrics.density, densityDpi")
            Text(text = "${displayMetrics.density}, ${displayMetrics.densityDpi} dpi")

            Label(text = "DisplayMetrics (xdpi, ydpi)")
            Text(text = "(${displayMetrics.xdpi}, ${displayMetrics.ydpi}) dpi")

            Text(
                text = "Resolution",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )

            Label(text = "constraints (maxWidth x maxHeight)")
            Text(
                text = "(" +
                    "${with(density) { constraints.maxWidth.toDp().value.roundToInt() }} x " +
                    "${with(density) { constraints.maxHeight.toDp().value.roundToInt() }}" +
                    ") dp, (${constraints.maxWidth} x ${constraints.maxHeight}) px",
            )

            Label(text = "Configuration (screenWidth x screenHeight)")
            Text(
                text = "(${configuration.screenWidthDp} x ${configuration.screenHeightDp}) dp, " +
                    "(" +
                    "${with(density) { configuration.screenWidthDp.dp.roundToPx() }} x " +
                    "${with(density) { configuration.screenHeightDp.dp.roundToPx() }}" +
                    ") px",
            )

            Label(text = "DisplayMetrics (width x height)")
            Text(
                text = "(" +
                    "${with(density) { displayMetrics.widthPixels.toDp().value.roundToInt() }} x " +
                    "${with(density) { displayMetrics.heightPixels.toDp().value.roundToInt() }}" +
                    ") dp, (${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}) px",
            )
        }
    }
}

@Composable
private fun Label(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelSmall,
    )
}
