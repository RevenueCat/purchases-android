package com.revenuecat.samplecat.ui.components

import android.graphics.BitmapFactory
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A background component with a gradient overlay and tiled noise pattern.
 *
 * Mimics the iOS ContentBackgroundView with a top-to-bottom gradient
 * that fades from a semi-transparent color to transparent.
 */
@Composable
fun ContentBackground(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Create a ShaderBrush for the tiled pattern
    val patternBrush = remember {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.noise_pattern)
            if (bitmap != null) {
                val shader = android.graphics.BitmapShader(
                    bitmap,
                    Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT,
                )
                ShaderBrush(shader)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Base noise pattern layer
        patternBrush?.let { brush ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = brush)
            }
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 400)
@Preview(
    showBackground = true,
    widthDp = 300,
    heightDp = 400,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ContentBackgroundPreview() {
    SampleCatTheme {
        ContentBackground(color = AccentColor)
    }
}
