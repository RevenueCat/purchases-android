@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.templates

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferDetails
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.localizedDiscount
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider

private object Template5UIConstants {
    val featureIconSize = 25.dp
    val checkmarkSize = 18.dp
    val discountPadding = 8.dp
    const val headerAspectRatio = 2f
    const val percentageScreenImageInLandscape = 0.4f
}

@Composable
internal fun Template5(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier.onGloballyPositioned { size = it.size },
    ) {
        var packageSelectorVisible by remember {
            mutableStateOf(state.templateConfiguration.mode != PaywallMode.FOOTER_CONDENSED)
        }

        if (state.isInFullScreenMode) {
            HeaderImage(state.templateConfiguration.images.headerUri, size)
        }

        Template5MainContent(state, viewModel, packageSelectorVisible)

        AnimatedVisibility(
            visible = packageSelectorVisible,
            enter = fadeIn(animationSpec = UIConstant.defaultAnimation()),
            exit = fadeOut(animationSpec = UIConstant.defaultAnimation()),
            label = "Template2.packageSpacing",
        ) {
            Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))
        }

        PurchaseButton(state, viewModel)

        Footer(
            templateConfiguration = state.templateConfiguration,
            viewModel = viewModel,
            allPlansTapped = { packageSelectorVisible = !packageSelectorVisible },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Template5MainContent(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    packageSelectionVisible: Boolean,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .conditional(state.isInFullScreenMode) {
                Modifier.verticalScroll(scrollState).weight(1f)
            }
            .padding(horizontal = UIConstant.defaultHorizontalPadding, vertical = UIConstant.defaultVerticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        if (state.isInFullScreenMode) {
            val localizedConfig = state.selectedLocalization
            val colors = state.templateConfiguration.getCurrentColors()
            Markdown(
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left,
                text = localizedConfig.title,
                color = colors.text1,
            )
            Spacer(Modifier.weight(1f))

            Features(
                features = localizedConfig.features,
                colors = colors,
            )

            Spacer(Modifier.weight(1f))
        }

        AnimatedPackages(state, packageSelectionVisible, viewModel)

        if (state.isInFullScreenMode) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderImage(uri: Uri?, templateSize: IntSize) {
    uri?.let {
        val aspectRatio = templateSize.aspectRatio
        RemoteImage(
            urlString = uri.toString(),
            modifier = Modifier
                .conditional(aspectRatio <= 1f || templateSize == IntSize.Zero) {
                    aspectRatio(ratio = Template5UIConstants.headerAspectRatio)
                }
                .conditional(aspectRatio > 1f) {
                    fillMaxHeight(Template5UIConstants.percentageScreenImageInLandscape).fillMaxWidth()
                },
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun Features(
    features: List<PaywallData.LocalizedConfiguration.Feature>,
    colors: TemplateConfiguration.Colors,
) {
    features.forEach { feature ->
        Feature(
            feature = feature,
            colors = colors,
        )
    }
}

@Composable
private fun Feature(
    feature: PaywallData.LocalizedConfiguration.Feature,
    colors: TemplateConfiguration.Colors,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            modifier = Modifier
                .size(Template5UIConstants.featureIconSize),
        ) {
            feature.iconID
                ?.let { PaywallIconName.fromValue(it) }
                ?.let { icon ->
                    PaywallIcon(
                        icon = icon,
                        tintColor = colors.featureIcon,
                    )
                }
        }

        Column(
            modifier = Modifier
                .padding(start = UIConstant.defaultHorizontalPadding),
        ) {
            Markdown(
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                text = feature.title,
                color = colors.text1,
            )
            feature.content?.let { content ->
                Markdown(
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    text = content,
                    color = colors.text2,
                )
            }
        }
    }
}

@Composable
private fun AnimatedPackages(
    state: PaywallState.Loaded,
    packageSelectionVisible: Boolean,
    viewModel: PaywallViewModel,
) {
    val packagesContentAlignment = if (state.isInFullScreenMode) {
        Alignment.TopStart
    } else {
        Alignment.BottomCenter
    }
    Box(contentAlignment = packagesContentAlignment) {
        AnimatedVisibility(
            visible = !packageSelectionVisible,
            enter = fadeIn(animationSpec = tween(delayMillis = UIConstant.defaultAnimationDurationMillis)),
            exit = fadeOut(animationSpec = tween(delayMillis = UIConstant.defaultAnimationDurationMillis)),
            label = "OfferDetailsVisibility",
        ) {
            OfferDetails(state)
        }

        AnimatedVisibility(
            visible = packageSelectionVisible,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
            label = "SelectPackagesVisibility",
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    UIConstant.defaultVerticalSpacing,
                    Alignment.CenterVertically,
                ),
            ) {
                state.templateConfiguration.packages.all.forEach { packageInfo ->
                    SelectPackageButton(state, packageInfo, viewModel)
                }
            }
        }
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun ColumnScope.SelectPackageButton(
    state: PaywallState.Loaded,
    packageInfo: TemplateConfiguration.PackageInfo,
    viewModel: PaywallViewModel,
) {
    val colors = state.currentColors
    val isSelected = packageInfo == state.selectedPackage.value

    val buttonAlpha = viewModel.packageButtonActionInProgressOpacityAnimation()
    val textColor = colors.text1
    val borderColor = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.selectedOutline,
        unselectedColor = colors.unselectedOutline,
    )

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(buttonAlpha)
            .align(Alignment.Start)
            .semantics {
                selected = isSelected
            },
        onClick = { viewModel.selectPackage(packageInfo) },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = textColor),
        shape = RoundedCornerShape(UIConstant.defaultPackageCornerRadius),
        contentPadding = PaddingValues(
            vertical = UIConstant.defaultVerticalSpacing,
            horizontal = UIConstant.defaultHorizontalPadding,
        ),
        border = BorderStroke(
            UIConstant.defaultPackageBorderWidth,
            borderColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckmarkBox(isSelected = isSelected, colors = colors)
                Text(
                    text = packageInfo.localization.offerName ?: packageInfo.rcPackage.product.title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                DiscountBanner(state = state, resourceProvider = viewModel.resourceProvider, packageInfo = packageInfo)
            }
            IntroEligibilityStateView(
                textWithNoIntroOffer = packageInfo.localization.offerDetails,
                textWithIntroOffer = packageInfo.localization.offerDetailsWithIntroOffer,
                textWithMultipleIntroOffers = packageInfo.localization.offerDetailsWithMultipleIntroOffers,
                eligibility = packageInfo.introEligibility,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                allowLinks = false,
            )
        }
    }
}

@Composable
private fun CheckmarkBox(isSelected: Boolean, colors: TemplateConfiguration.Colors) {
    Box(
        modifier = Modifier
            .size(Template5UIConstants.checkmarkSize)
            .clip(CircleShape)
            .background(if (isSelected) colors.background else colors.unselectedOutline),
    ) {
        if (isSelected) {
            PaywallIcon(
                icon = PaywallIconName.CHECK_CIRCLE,
                tintColor = colors.selectedOutline,
            )
        }
    }
}

@Composable
private fun DiscountBanner(
    state: PaywallState.Loaded,
    resourceProvider: ResourceProvider,
    packageInfo: TemplateConfiguration.PackageInfo,
) {
    val text = packageInfo.localizedDiscount(resourceProvider)?.uppercase() ?: return

    val colors = state.currentColors

    val backgroundColor = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.selectedOutline,
        unselectedColor = colors.unselectedOutline,
    )
    val foregroundColor = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.selectedDiscountText,
        unselectedColor = colors.unselectedDiscountText,
    )

    Box(
        modifier = Modifier
            .offset(
                x = UIConstant.defaultHorizontalPadding - Template5UIConstants.discountPadding,
                y = -UIConstant.defaultVerticalSpacing + Template5UIConstants.discountPadding,
            ),
    ) {
        Box(
            Modifier
                .background(color = backgroundColor, shape = ButtonDefaults.shape)
                .padding(vertical = 4.dp)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = text,
                color = foregroundColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

private val TemplateConfiguration.Colors.featureIcon: Color
    get() = this.accent1
private val TemplateConfiguration.Colors.selectedOutline: Color
    get() = this.accent2
private val TemplateConfiguration.Colors.unselectedOutline: Color
    get() = this.accent3
private val TemplateConfiguration.Colors.selectedDiscountText: Color
    get() = this.text2
private val TemplateConfiguration.Colors.unselectedDiscountText: Color
    get() = this.text3

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Preview(showBackground = true, widthDp = 1000, heightDp = 1000)
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template5PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template5Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
private fun Template5PaywallFooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template5Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
private fun Template5PaywallFooterCondensedPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template5Offering),
    )
}
