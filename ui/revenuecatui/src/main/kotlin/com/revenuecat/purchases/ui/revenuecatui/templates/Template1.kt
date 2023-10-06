package com.revenuecat.purchases.ui.revenuecatui.templates

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import kotlin.math.roundToInt

@Composable
internal fun Template1(state: PaywallViewState.Loaded, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Template1MainContent(state)
        }
        PurchaseButton(state, viewModel)
        Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
    }
}

@Composable
private fun ColumnScope.Template1MainContent(state: PaywallViewState.Loaded) {
    val localizedConfig = state.selectedLocalization
    val colors = state.currentColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        HeaderImage(state.templateConfiguration.images.headerUri)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            text = localizedConfig.title,
            color = colors.text1,
            modifier = Modifier
                .padding(
                    horizontal = UIConstant.defaultHorizontalPadding,
                    vertical = UIConstant.defaultVerticalSpacing,
                ),
        )

        Box(
            modifier = Modifier
                .padding(horizontal = UIConstant.defaultHorizontalPadding),
        ) {
            Text(
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                text = localizedConfig.subtitle ?: "",
                color = colors.text1,
            )
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        IntroEligibilityStateView(
            textWithNoIntroOffer = localizedConfig.offerDetails,
            textWithIntroOffer = localizedConfig.offerDetailsWithIntroOffer,
            eligibility = state.selectedPackage.introEligibility,
            color = colors.text1,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeaderImage(uri: Uri?) {
    uri?.let {
        CircleMask {
            RemoteImage(
                urlString = uri.toString(),
                modifier = Modifier
                    .aspectRatio(ratio = 1.2f),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CircleMask(content: @Composable () -> Unit) {
    var size by remember { mutableStateOf(IntSize(0, 0)) }

    fun circleOffset(size: IntSize): Int {
        return ((size.width * Template1UIConstants.circleScale - size.width) / 2.0 * -1).roundToInt()
    }

    Box(
        modifier = Modifier
            .onSizeChanged { size = it }
            .graphicsLayer(
                translationY = circleOffset(size).toFloat(),
                scaleX = Template1UIConstants.circleScale,
                scaleY = Template1UIConstants.circleScale,
            )
            .clip(shape = CircleShape),
    ) {
        Column {
            content()
        }
    }
}

private object Template1UIConstants {
    const val circleScale = 3.0f
}

@Preview(showBackground = true)
@Composable
internal fun Template1PaywallPreview() {
    InternalPaywallView(options = PaywallViewOptions.Builder().setOffering(TestData.template1Offering).build())
}

@Preview(heightDp = 700, widthDp = 400)
@Composable
internal fun CircleMaskPreview() {
    Box {
        Box(
            modifier = Modifier
                .background(color = Color.Red)
                .fillMaxSize(),
        )
        CircleMask {
            Box(
                modifier = Modifier
                    .background(color = Color.Blue)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.5f),
            )
        }
    }
}
