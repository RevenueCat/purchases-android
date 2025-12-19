package com.revenuecat.sample.admob.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.nativead.NativeAd
import com.revenuecat.sample.admob.data.Constants
import com.revenuecat.sample.admob.manager.AdMobManager

/**
 * Main screen demonstrating AdMob integration with RevenueCat ad event tracking.
 *
 * This screen showcases:
 * 1. Banner Ad - Always visible at the top
 * 2. Interstitial Ad - Loaded and shown via button
 * 3. Native Ad - Loaded and displayed via button
 * 4. Native Video Ad - Loaded and displayed via button
 * 5. Error Testing - Demonstrates ad load failure tracking
 *
 * All ads track the following RevenueCat events:
 * - trackAdLoaded
 * - trackAdDisplayed
 * - trackAdOpened (on click)
 * - trackAdRevenue (via OnPaidEventListener)
 * - trackAdFailedToLoad (for error scenarios)
 */
@Composable
fun HomeScreen(
    activity: Activity,
    adMobManager: AdMobManager
) {
    val context = LocalContext.current
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var nativeVideoAdState by remember { mutableStateOf<NativeAd?>(null) }
    var interstitialStatus by remember { mutableStateOf("Not Loaded") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "AdMob + RevenueCat\nIntegration Sample",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "This app demonstrates tracking all 5 RevenueCat ad events across 4 AdMob ad formats. " +
                    "Check Logcat for detailed event tracking logs.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ============================================================================
        // Section 1: Banner Ad
        // ============================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "1. Banner Ad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Always visible at the top. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
                    style = MaterialTheme.typography.bodySmall
                )

                // Banner Ad Composable
                BannerAdView(
                    adSize = AdSize.BANNER,
                    adUnitId = Constants.AdMob.BANNER_AD_UNIT_ID,
                    adMobManager = adMobManager,
                    placement = "home_banner"
                )
            }
        }

        // ============================================================================
        // Section 2: Interstitial Ad
        // ============================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "2. Interstitial Ad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Full-screen ad. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Status: $interstitialStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = {
                        interstitialStatus = "Loading..."
                        adMobManager.loadInterstitialAd(
                            placement = "home_interstitial",
                            onAdLoaded = {
                                interstitialStatus = "Loaded - Ready to Show"
                                Toast.makeText(context, "Interstitial loaded!", Toast.LENGTH_SHORT).show()
                            },
                            onAdFailedToLoad = { error ->
                                interstitialStatus = "Failed: $error"
                                Toast.makeText(context, "Failed to load", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Interstitial Ad")
                }

                Button(
                    onClick = {
                        val shown = adMobManager.showInterstitialAd(activity)
                        if (shown) {
                            interstitialStatus = "Shown - Load Again"
                        } else {
                            Toast.makeText(context, "No ad loaded yet", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = interstitialStatus.contains("Loaded")
                ) {
                    Text("Show Interstitial Ad")
                }
            }
        }

        // ============================================================================
        // Section 3: Native Ad
        // ============================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. Native Ad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Integrated content ad (text + images). Tracks: Loaded, Displayed, Opened (on click), Revenue.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "⚠️ Note: Google's test native ad IDs have known issues. " +
                            "The implementation is correct - use your own AdMob ad unit ID for testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = {
                        adMobManager.loadNativeAd(
                            adUnitId = Constants.AdMob.NATIVE_AD_UNIT_ID,
                            placement = "home_native",
                            onAdLoaded = { nativeAd ->
                                nativeAdState = nativeAd
                                // Track displayed after rendering
                                adMobManager.trackNativeAdDisplayed(
                                    Constants.AdMob.NATIVE_AD_UNIT_ID,
                                    "home_native",
                                    nativeAd
                                )
                                Toast.makeText(context, "Native ad loaded!", Toast.LENGTH_SHORT).show()
                            },
                            onAdFailedToLoad = { error ->
                                Toast.makeText(context, "Native ad failed: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Native Ad")
                }

                // Display native ad if loaded
                nativeAdState?.let { nativeAd ->
                    NativeAdView(nativeAd = nativeAd)
                }
            }
        }

        // ============================================================================
        // Section 4: Native Video Ad
        // ============================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "4. Native Video Ad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Integrated content ad with video. Tracks: Loaded, Displayed, Opened (on click), Revenue.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "⚠️ Note: Google's test native ad IDs have known issues. " +
                            "The implementation is correct - use your own AdMob ad unit ID for testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = {
                        adMobManager.loadNativeAd(
                            adUnitId = Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                            placement = "home_native_video",
                            onAdLoaded = { nativeAd ->
                                nativeVideoAdState = nativeAd
                                // Track displayed after rendering
                                adMobManager.trackNativeAdDisplayed(
                                    Constants.AdMob.NATIVE_VIDEO_AD_UNIT_ID,
                                    "home_native_video",
                                    nativeAd
                                )
                                Toast.makeText(context, "Native video ad loaded!", Toast.LENGTH_SHORT).show()
                            },
                            onAdFailedToLoad = { error ->
                                Toast.makeText(context, "Native video ad failed: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Native Video Ad")
                }

                // Display native video ad if loaded
                nativeVideoAdState?.let { nativeAd ->
                    NativeAdView(nativeAd = nativeAd)
                }
            }
        }

        // ============================================================================
        // Section 5: Error Testing
        // ============================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "5. Error Testing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Uses invalid ad unit ID to trigger and track ad load failure.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        adMobManager.loadAdWithError(placement = "error_test")
                        Toast.makeText(
                            context,
                            "Loading with invalid ID - check logs for failure tracking",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger Ad Load Error")
                }
            }
        }

        // Footer
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "💡 Tip: Open Logcat and filter by 'AdMobManager' or 'RevenueCat' " +
                    "to see all ad event tracking in real-time.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
