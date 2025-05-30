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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldUseLandscapeLayout

@Composable
internal fun Template1(state: PaywallState.Loaded.Legacy, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Template1MainContent(state)
        PurchaseButton(state, viewModel)
        Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun ColumnScope.Template1MainContent(state: PaywallState.Loaded.Legacy) {
    val localizedConfig = state.selectedLocalization
    val colors = state.currentColors

    if (state.isInFullScreenMode) {
        val landscapeLayout = state.shouldUseLandscapeLayout()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .fillMaxHeight()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeaderImage(state.templateConfiguration.images.headerUri, landscapeLayout = landscapeLayout)

            Spacer(modifier = Modifier.weight(1f))

            Markdown(
                text = localizedConfig.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = colors.text1,
                textFillMaxWidth = true,
                applyFontSizeToParagraph = false,
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
                    textFillMaxWidth = true,
                    applyFontSizeToParagraph = false,
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
private fun HeaderImage(uri: Uri?, landscapeLayout: Boolean) {
    uri?.let {
        CircleMask(landscapeLayout = landscapeLayout) {
            val screenHeight = LocalConfiguration.current.screenHeightDp
            RemoteImage(
                urlString = uri.toString(),
                modifier = Modifier
                    .conditional(!landscapeLayout) {
                        aspectRatio(ratio = 1.2f)
                    }
                    .conditional(landscapeLayout) {
                        fillMaxWidth().height(screenHeight.dp / 2f)
                    },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CircleMask(landscapeLayout: Boolean, content: @Composable () -> Unit) {
    val scale = if (landscapeLayout) {
        Template1UIConstants.circleScaleLanscape
    } else {
        Template1UIConstants.circleScale
    }

    fun circleOffsetX(size: Size): Float {
        return ((size.width * scale) - size.width) / 2f * -1f
    }

    fun circleOffsetY(size: Size): Float {
        return ((size.height * scale) - size.height) * -1f
    }

    val clipShape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val matrix = Matrix()
            matrix.preScale(scale, scale)
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
    const val circleScaleLanscape = 8.0f
}

@Preview(showBackground = true, group = "full_screen")
@Preview(
    showBackground = true,
    group = "full_screen",
    name = "Landscape",
    widthDp = 720,
    heightDp = 380,
)
@Preview(showBackground = true, device = Devices.NEXUS_7, group = "full_screen")
@Preview(showBackground = true, device = Devices.NEXUS_10, group = "full_screen")
@Composable
private fun Template1PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template1Offering),
    )
}

@Preview(showBackground = true, group = "full_screen")
@Composable
private fun Template1NoFooterPaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template1OfferingNoFooter),
    )
}

@Preview(showBackground = true, group = "footer")
@Composable
private fun Template1FooterPaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template1Offering),
    )
}

@Preview(showBackground = true, group = "condensed")
@Composable
private fun Template1CondensedFooterPaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template1Offering),
    )
}

@Preview(heightDp = 700, widthDp = 400, group = "mask")
@Composable
private fun CircleMaskPreview() {
    Box {
        Box(
            modifier = Modifier
                .background(color = Color.Red)
                .fillMaxSize(),
        )
        CircleMask(landscapeLayout = false) {
            Box(
                modifier = Modifier
                    .background(color = Color.Blue)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.5f),
            )
        }
    }
}
