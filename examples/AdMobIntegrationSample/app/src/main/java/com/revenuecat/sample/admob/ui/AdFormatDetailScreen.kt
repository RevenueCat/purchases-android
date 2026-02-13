package com.revenuecat.sample.admob.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.admob.ui.ads.AppOpenAdContent
import com.revenuecat.sample.admob.ui.ads.BannerAdContent
import com.revenuecat.sample.admob.ui.ads.ErrorTestingContent
import com.revenuecat.sample.admob.ui.ads.InterstitialAdContent
import com.revenuecat.sample.admob.ui.ads.NativeAdContent
import com.revenuecat.sample.admob.ui.ads.NativeVideoAdContent
import com.revenuecat.sample.admob.ui.ads.RewardedAdContent
import com.revenuecat.sample.admob.ui.ads.RewardedInterstitialAdContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdFormatDetailScreen(
    format: AdFormat,
    activity: Activity,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(format.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (format) {
                AdFormat.BANNER -> BannerAdContent()
                AdFormat.INTERSTITIAL -> InterstitialAdContent(activity)
                AdFormat.APP_OPEN -> AppOpenAdContent(activity)
                AdFormat.REWARDED -> RewardedAdContent(activity)
                AdFormat.REWARDED_INTERSTITIAL -> RewardedInterstitialAdContent(activity)
                AdFormat.NATIVE -> NativeAdContent()
                AdFormat.NATIVE_VIDEO -> NativeVideoAdContent()
                AdFormat.ERROR_TESTING -> ErrorTestingContent()
            }
        }
    }
}
