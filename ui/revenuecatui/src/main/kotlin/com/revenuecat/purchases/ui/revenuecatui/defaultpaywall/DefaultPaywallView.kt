package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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

@Composable
@Suppress("LongMethod")
internal fun DefaultPaywallView(
    packages: List<Package>,
    warning: PaywallWarning?,
    onPurchase: (Package) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    // Check if the app is in debug mode
    val isDebugBuild = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // App metadata
    val appName = remember { AppStyleExtractor.getAppName(context) }
    var appIconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Color extraction state
    var prominentColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(Unit) {
        val bitmap = withContext(Dispatchers.Default) {
            AppStyleExtractor.getAppIconBitmap(context)
        }
        val extractedProminentColors = AppStyleExtractor.getProminentColorsFromBitmap(bitmap, count = 2)

        appIconBitmap = bitmap
        prominentColors = extractedProminentColors
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

    // Main layout
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 630.dp)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .heightIn(min = maxHeight)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        if (shouldShowWarning) {
                            Text(
                                text = stringResource(R.string.revenuecatui_paywalls_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (warningToShow != null) {
                            DefaultPaywallWarning(
                                warning = warningToShow,
                                warningColor = RevenueCatBrandRed,
                            )
                        } else {
                            AppIconSection(
                                bitmap = appIconBitmap,
                                appName = appName,
                                shadowColor = mainColor,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        packages.forEach { pkg ->
                            DefaultProductCell(
                                pkg = pkg,
                                accentColor = mainColor,
                                selectedFontColor = foregroundOnAccentColor,
                                isSelected = selectedPackage == pkg,
                                onSelect = { selectedPackage = pkg },
                            )
                        }
                    }
                }
            }

            if (packages.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = { selectedPackage?.let(onPurchase) },
                        enabled = selectedPackage != null,
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
