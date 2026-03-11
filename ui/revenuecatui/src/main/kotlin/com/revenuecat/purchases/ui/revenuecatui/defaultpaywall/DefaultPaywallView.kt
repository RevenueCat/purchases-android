package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.helpers.AppStyleExtractor
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallWarning
import com.revenuecat.purchases.ui.revenuecatui.helpers.selectColorWithBestContrast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("MagicNumber")
private val RevenueCatBrandRed = Color(0xFFF2545B)

internal data class DefaultPaywallPreviewOverrides(
    val appName: String? = null,
    val appIconBitmap: Bitmap? = null,
    val prominentColors: List<Color>? = null,
    val isDebugBuild: Boolean? = null,
)

@Composable
@Suppress("LongMethod", "COMPOSE_APPLIER_CALL_MISMATCH")
internal fun DefaultPaywallView(
    packages: List<Package>,
    warning: PaywallWarning?,
    onPurchase: (Package) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    previewOverrides: DefaultPaywallPreviewOverrides? = null,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val isDebugBuild =
        previewOverrides?.isDebugBuild ?: remember {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

    // App metadata
    val appName = previewOverrides?.appName ?: remember { AppStyleExtractor.getAppName(context) }
    var appIconBitmap by remember { mutableStateOf(previewOverrides?.appIconBitmap) }
    val providedProminentColors = previewOverrides?.prominentColors

    // Color extraction state
    var prominentColors by remember(appIconBitmap, providedProminentColors) {
        mutableStateOf(providedProminentColors ?: emptyList())
    }

    LaunchedEffect(appIconBitmap, providedProminentColors) {
        if (providedProminentColors == null) {
            prominentColors = AppStyleExtractor.getProminentColorsFromBitmap(appIconBitmap, count = 2)
        }

        if (appIconBitmap == null) {
            val bitmap = withContext(Dispatchers.Default) {
                AppStyleExtractor.getAppIconBitmap(context)
            }
            appIconBitmap = bitmap
        }
    }

    // Selection state
    var selectedPackage by remember(packages) { mutableStateOf(packages.firstOrNull()) }

    // Determine if we should show the warning (DEBUG only)
    val warningToShow = warning.takeIf { isDebugBuild }
    val shouldShowWarning = warningToShow != null

    // Calculate colors
    val iconColor = if (prominentColors.isEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        // Keep a usable accent/background color even if extraction finds no distinct contrast winner.
        selectColorWithBestContrast(
            from = prominentColors,
            againstColor = if (isDarkTheme) Color.Black else Color.White,
        ) ?: MaterialTheme.colorScheme.primary
    }

    val mainColor = if (shouldShowWarning) RevenueCatBrandRed else iconColor

    val foregroundOnAccentColor = if (shouldShowWarning) {
        Color.White
    } else {
        // Always fall back to a valid content color for the chosen background/accent color.
        selectColorWithBestContrast(
            from = prominentColors + listOf(if (isDarkTheme) Color.Black else Color.White),
            againstColor = iconColor,
        ) ?: MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        mainColor.copy(alpha = 0.2f),
                        mainColor.copy(alpha = 0f),
                    ),
                ),
            ),
    ) {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (packages.isNotEmpty()) {
                    DefaultPaywallFooter(
                        onPurchase = { selectedPackage?.let(onPurchase) },
                        onRestore = onRestore,
                        purchaseEnabled = selectedPackage != null,
                        mainColor = mainColor,
                        foregroundOnAccentColor = foregroundOnAccentColor,
                    )
                }
            },
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = ReadableContentWidth.dp)
                    .align(Alignment.TopCenter),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    if (shouldShowWarning) {
                        Text(
                            text = stringResource(R.string.revenuecatui_paywalls_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (warningToShow != null) {
                        DefaultPaywallWarning(warning = warningToShow, warningColor = RevenueCatBrandRed)
                    } else {
                        AppIconSection(
                            bitmap = appIconBitmap,
                            appName = appName,
                            shadowColor = mainColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                LazyColumn(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = packages,
                        key = { it.identifier },
                    ) { pkg ->
                        DefaultProductCell(
                            pkg = pkg,
                            accentColor = mainColor,
                            selectedFontColor = foregroundOnAccentColor,
                            isSelected = selectedPackage == pkg,
                            onSelect = { selectedPackage = pkg },
                        )
                    }

                    if (packages.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultPaywallFooter(
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    purchaseEnabled: Boolean,
    mainColor: Color,
    foregroundOnAccentColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ReadableContentWidth.dp)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onPurchase,
                enabled = purchaseEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainColor,
                    contentColor = foregroundOnAccentColor,
                ),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.revenuecatui_purchase),
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onRestore,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(text = stringResource(R.string.revenuecatui_restore_purchases))
            }
        }
    }
}

@Composable
private fun AppIconSection(
    bitmap: Bitmap?,
    appName: String,
    shadowColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        bitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(31.dp),
                        ambientColor = shadowColor.copy(alpha = 0.2f),
                        spotColor = shadowColor.copy(alpha = 0.2f),
                    ),
            ) {
                // Main icon
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "App Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(31.dp)),
                )
            }
        }

        Text(
            text = appName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private object ReadableContentWidth {
    val dp = 630.dp
}
