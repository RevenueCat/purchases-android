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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
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
import com.revenuecat.purchases.ui.revenuecatui.composables.PillSwitcher
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.composables.StatusBarSpacer
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.localizedDiscount
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldUseLandscapeLayout

private object Template7UIConstants {
    val featureIconSize = 25.dp
    val checkmarkSize = 18.dp
    val discountPadding = 8.dp
    const val headerAspectRatio = 2f
}

@Composable
internal fun Template7(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    var packageSelectorVisible by remember {
        mutableStateOf(state.templateConfiguration.mode != PaywallMode.FOOTER_CONDENSED)
    }

    val colorForTier = state.templateConfiguration.getCurrentColorsForTier(tier = state.selectedTier.value)

    Column {
        if (state.shouldUseLandscapeLayout()) {
            Template7LandscapeContent(state, viewModel)
        } else {
            Template7PortraitContent(state, viewModel, packageSelectorVisible)
        }

        PurchaseButton(state, viewModel, colors = colorForTier)

        // TODO: Add to footer
        Footer(
            templateConfiguration = state.templateConfiguration,
            viewModel = viewModel,
            allPlansTapped = { packageSelectorVisible = !packageSelectorVisible },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Template7PortraitContent(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    packageSelectionVisible: Boolean,
) {
    val headerUri = state.selectedImages.headerUri
    if (state.isInFullScreenMode && headerUri != null) {
        HeaderImage(headerUri)
    }

    val colorForTier = state.templateConfiguration.getCurrentColorsForTier(tier = state.selectedTier.value)

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .conditional(state.isInFullScreenMode) {
                Modifier
                    .verticalScroll(scrollState)
                    .weight(1f)
            }
            .padding(horizontal = UIConstant.defaultHorizontalPadding, vertical = UIConstant.defaultVerticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        if (state.isInFullScreenMode) {
            if (headerUri == null) {
                if (!state.shouldDisplayDismissButton) {
                    StatusBarSpacer()
                }
                Spacer(Modifier.height(UIConstant.iconButtonSize))
            }
            Title(state, state.selectedTier.value)

            PillSwitcher(
                options = state.templateConfiguration.configuration.tiers ?: emptyList(),
                optionNames = state.templateConfiguration.configuration.tiers?.associate {
                    it to (state.templateConfiguration.packagesByTier[it.id]?.default?.localization?.tierName ?: "") // TODO: This is also bad
                } ?: emptyMap(),
                selectedOption = state.selectedTier.value,
                onOptionSelected = {
                                   state.selectTier(it)
                },
                backgroundColor = colorForTier.accent1
            )

            Features(state, state.selectedTier.value)
        }

        AnimatedPackages(
            state = state,
            viewModel = viewModel,
            packages = state.templateConfiguration.packagesByTier[state.selectedTier.value.id]?.all ?: emptyList(), // TODO: Fix, this is bad
            colors = colorForTier
        )

        if (state.isInFullScreenMode) {
            Spacer(Modifier.weight(1f))
        }
    }

    AnimatedVisibility(
        visible = packageSelectionVisible,
        enter = fadeIn(animationSpec = UIConstant.defaultAnimation()),
        exit = fadeOut(animationSpec = UIConstant.defaultAnimation()),
        label = "Template7.packageSpacing",
    ) {
        Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))
    }
}

@Composable
private fun ColumnScope.Template7LandscapeContent(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    val colorForTier = state.templateConfiguration.getCurrentColorsForTier(tier = state.selectedTier.value)

    Row(
        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1.0f)
            .padding(top = UIConstant.defaultVerticalSpacing)
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(leftScrollState)
                .weight(1f)
                .padding(
                    horizontal = UIConstant.defaultHorizontalPadding,
                    vertical = UIConstant.defaultVerticalSpacing,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.weight(UIConstant.halfWeight))

            Title(state, state.selectedTier.value)

            Spacer(Modifier.weight(UIConstant.halfWeight))

            Features(state, state.selectedTier.value)
        }

        Column(
            modifier = Modifier
                .verticalScroll(rightScrollState)
                .weight(1f)
                .padding(
                    horizontal = UIConstant.defaultHorizontalPadding,
                    vertical = UIConstant.defaultVerticalSpacing,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.weight(UIConstant.halfWeight))

            PillSwitcher(
                options = state.templateConfiguration.configuration.tiers ?: emptyList(),
                optionNames = state.templateConfiguration.configuration.tiers?.associate {
                    it to (state.templateConfiguration.packagesByTier[it.id]?.default?.localization?.tierName ?: "") // TODO: This is also bad
                } ?: emptyMap(),
                selectedOption = state.selectedTier.value,
                onOptionSelected = {
                    state.selectTier(it)
                },
                backgroundColor = colorForTier.accent1
            )

            Spacer(Modifier.weight(UIConstant.halfWeight))

            AnimatedPackages(
                state = state,
                viewModel = viewModel,
                packages = state.templateConfiguration.packagesByTier[state.selectedTier.value.id]?.all ?: emptyList(), // TODO: Fix, this is bad
                colors = colorForTier
            )

            Spacer(Modifier.weight(UIConstant.halfWeight))
        }
    }
}

@Composable
private fun HeaderImage(uri: Uri?) {
    uri?.let {
        RemoteImage(
            urlString = uri.toString(),
            modifier = Modifier
                .aspectRatio(ratio = Template7UIConstants.headerAspectRatio),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ColumnScope.Title(
    state: PaywallState.Loaded,
    selectedTier: PaywallData.Configuration.Tier,
) {
    Markdown(
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        text = state.selectedLocalizationForTier.title,
        color = state.templateConfiguration.getCurrentColorsForTier(tier = selectedTier).text1,
        modifier = Modifier
            .fillMaxWidth(),
    )
}

@Composable
private fun Features(
    state: PaywallState.Loaded,
    selectedTier: PaywallData.Configuration.Tier,
) {
    val colors = state.templateConfiguration.getCurrentColorsForTier(tier = selectedTier)

    state.selectedLocalizationForTier.features.forEach { feature ->
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
                .size(Template7UIConstants.featureIconSize),
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
    viewModel: PaywallViewModel,
    packageSelectionVisible: Boolean = true,
    packages: List<TemplateConfiguration.PackageInfo>,
    colors: TemplateConfiguration.Colors,
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
                packages.forEach { packageInfo ->
                    SelectPackageButton(state, packageInfo, viewModel, colors)
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
    colors: TemplateConfiguration.Colors
) {
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
                    modifier = Modifier.weight(1f, fill = true),
                )

                DiscountBanner(
                    state = state,
                    resourceProvider = viewModel.resourceProvider,
                    packageInfo = packageInfo,
                    colors = colors
                )
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
            .size(Template7UIConstants.checkmarkSize)
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
private fun RowScope.DiscountBanner(
    state: PaywallState.Loaded,
    resourceProvider: ResourceProvider,
    packageInfo: TemplateConfiguration.PackageInfo,
    colors: TemplateConfiguration.Colors
) {
    val text = packageInfo.localizedDiscount(resourceProvider)?.uppercase() ?: return

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
            .align(Alignment.Top)
            .offset(
                x = UIConstant.defaultHorizontalPadding - Template7UIConstants.discountPadding,
                y = -UIConstant.defaultVerticalSpacing + Template7UIConstants.discountPadding,
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

// TODO: This is bad
private val PaywallState.Loaded.selectedImages: TemplateConfiguration.Images
    get() = selectedTier.value.let { templateConfiguration.imagesByTier[it.id] }!!

internal val PaywallState.Loaded.selectedLocalizationForTier: ProcessedLocalizedConfiguration
    get() = templateConfiguration.packagesByTier[selectedTier.value.id]!!.default.localization // TODO: This is wrong

private val TemplateConfiguration.Colors.featureIcon: Color
    get() = this.text1
private val TemplateConfiguration.Colors.selectedOutline: Color
    get() = this.accent3
private val TemplateConfiguration.Colors.unselectedOutline: Color
    get() = this.accent2
private val TemplateConfiguration.Colors.selectedDiscountText: Color
    get() = this.text2
private val TemplateConfiguration.Colors.unselectedDiscountText: Color
    get() = this.text3

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS", group = "full_screen")
@Preview(
    showBackground = true,
    locale = "en-rUS",
    group = "full_screen",
    name = "Landscape",
    widthDp = 720,
    heightDp = 380,
)
@Preview(showBackground = true, locale = "es-rES", group = "full_screen")
@Preview(showBackground = true, widthDp = 1000, heightDp = 1000, group = "full_screen")
@Preview(showBackground = true, device = Devices.NEXUS_7, group = "full_screen")
@Preview(showBackground = true, device = Devices.NEXUS_10, group = "full_screen")
@Composable
private fun Template7PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template7Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS", group = "footer")
@Preview(showBackground = true, locale = "es-rES", group = "footer")
@Composable
private fun Template7PaywallFooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template7Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS", group = "condensed")
@Preview(showBackground = true, locale = "es-rES", group = "condensed")
@Composable
private fun Template7PaywallFooterCondensedPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template7Offering),
    )
}
