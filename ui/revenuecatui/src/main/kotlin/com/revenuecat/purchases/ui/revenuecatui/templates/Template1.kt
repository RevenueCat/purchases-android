package com.revenuecat.purchases.ui.revenuecatui.templates

import android.graphics.Matrix
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferDetails
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun Template1(state: PaywallState.Loaded, viewModel: PaywallViewModel) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier.fillMaxWidth().onGloballyPositioned { size = it.size },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Template1MainContent(state, size)
        PurchaseButton(state, viewModel)
        Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun ColumnScope.Template1MainContent(state: PaywallState.Loaded, template1Size: IntSize) {
    val localizedConfig = state.selectedLocalization
    val colors = state.currentColors

    if (state.isInFullScreenMode) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .fillMaxHeight()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeaderImage(state.templateConfiguration.images.headerUri, template1Size)

            Spacer(modifier = Modifier.weight(1f))

            Markdown(
                text = localizedConfig.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
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
                Markdown(
                    text = localizedConfig.subtitle ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(
                            horizontal = UIConstant.defaultHorizontalPadding,
                            vertical = UIConstant.defaultVerticalSpacing,
                        ),
                    color = colors.text1,
                )
            }

            Spacer(modifier = Modifier.weight(2f))
        }
    } else {
        Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))
    }

    OfferDetails(state)
}

@Composable
private fun HeaderImage(uri: Uri?, templateSize: IntSize) {
    uri?.let {
        CircleMask {
            val aspectRatio = templateSize.aspectRatio
            val screenHeight = LocalConfiguration.current.screenHeightDp
            RemoteImage(
                urlString = uri.toString(),
                modifier = Modifier
                    .conditional(aspectRatio <= 1f || templateSize == IntSize.Zero) {
                        aspectRatio(ratio = 1.2f)
                    }
                    .conditional(aspectRatio > 1f) {
                        fillMaxWidth().height(screenHeight.dp / 2f)
                    },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CircleMask(content: @Composable () -> Unit) {
    fun circleOffsetX(size: Size): Float {
        return ((size.width * Template1UIConstants.circleScale) - size.width) / 2f * -1f
    }
    fun circleOffsetY(size: Size): Float {
        return ((size.height * Template1UIConstants.circleScale) - size.height) * -1f
    }

    val clipShape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val matrix = Matrix()
            matrix.preScale(Template1UIConstants.circleScale, Template1UIConstants.circleScale)
            matrix.postTranslate(circleOffsetX(size), circleOffsetY(size))

            return Outline.Generic(
                Path().apply {
                    addOval(Rect(offset = Offset.Zero, size))
                    asAndroidPath().transform(matrix)
                },
            )
        }
    }

    Box(modifier = Modifier.clip(shape = clipShape)) {
        content()
    }
}

private object Template1UIConstants {
    const val circleScale = 3.0f
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template1PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template1Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true)
@Composable
private fun Template1FooterPaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template1Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true)
@Composable
private fun Template1CondensedFooterPaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template1Offering),
    )
}

@Preview(heightDp = 700, widthDp = 400)
@Composable
private fun CircleMaskPreview() {
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
