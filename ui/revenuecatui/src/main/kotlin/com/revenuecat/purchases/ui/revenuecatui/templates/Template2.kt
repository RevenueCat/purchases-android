@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.templates

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IconImage
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferDetails
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.StatusBarSpacer
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation
import com.revenuecat.purchases.ui.revenuecatui.helpers.TestTag
import com.revenuecat.purchases.ui.revenuecatui.helpers.shouldUseLandscapeLayout

private object Template2UIConstants {
    val maxIconWidth = 140.dp
    val iconCornerRadius = 16.dp
    val checkmarkSize = 18.dp
    const val fadedColorOpacity = 0.3f
}

/**
 * @param childModifier this allows using [Modifier.placeholder].
 */
@Composable
internal fun Template2(
    state: PaywallState.Loaded.Legacy,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
) {
    Box {
        PaywallBackground(state.templateConfiguration)

        Column(
            verticalArrangement = if (state.isInFullScreenMode) Arrangement.SpaceAround else Arrangement.Top,
        ) {
            StatusBarSpacer()
            var packageSelectorVisible by remember {
                mutableStateOf(state.templateConfiguration.mode != PaywallMode.FOOTER_CONDENSED)
            }

            if (state.shouldUseLandscapeLayout()) {
                Template2LandscapeContent(state, viewModel, packageSelectorVisible, childModifier)
            } else {
                Template2PortraitContent(state, viewModel, packageSelectorVisible, childModifier)

                AnimatedVisibility(
                    visible = packageSelectorVisible,
                    enter = fadeIn(animationSpec = UIConstant.defaultAnimation()),
                    exit = fadeOut(animationSpec = UIConstant.defaultAnimation()),
                    label = "Template2.packageSpacing",
                ) {
                    Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))
                }

                PurchaseButton(state, viewModel, childModifier)
            }

            Footer(
                templateConfiguration = state.templateConfiguration,
                viewModel = viewModel,
                childModifier = childModifier,
                allPlansTapped = { packageSelectorVisible = !packageSelectorVisible },
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Template2PortraitContent(
    state: PaywallState.Loaded.Legacy,
    viewModel: PaywallViewModel,
    packageSelectionVisible: Boolean,
    childModifier: Modifier,
) {
    Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))

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
            Spacer(Modifier.weight(1f))

            IconImage(state, childModifier)

            Title(state, childModifier)

            Spacer(Modifier.weight(1f))

            Subtitle(state, childModifier)

            Spacer(Modifier.weight(1f))
        }

        AnimatedPackages(
            state,
            packageSelectionVisible,
            landscapeLayout = false,
            viewModel,
            childModifier,
        )

        if (state.isInFullScreenMode) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Template2LandscapeContent(
    state: PaywallState.Loaded.Legacy,
    viewModel: PaywallViewModel,
    packageSelectionVisible: Boolean,
    childModifier: Modifier,
) {
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1.0f)
            .padding(horizontal = UIConstant.defaultHorizontalPadding, vertical = UIConstant.defaultVerticalSpacing),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(leftScrollState)
                .weight(UIConstant.halfWeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.weight(UIConstant.halfWeight))

            IconImage(state, childModifier)

            Title(state, childModifier, TextAlign.Start)

            Spacer(Modifier.weight(UIConstant.halfWeight))

            Subtitle(state, childModifier, TextAlign.Start)

            Spacer(Modifier.weight(UIConstant.halfWeight))
        }

        Column(
            modifier = Modifier
                .verticalScroll(rightScrollState)
                .weight(UIConstant.halfWeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.weight(UIConstant.halfWeight))

            AnimatedPackages(
                state,
                packageSelectionVisible,
                landscapeLayout = true,
                viewModel,
                childModifier,
            )

            Spacer(Modifier.weight(UIConstant.halfWeight))

            PurchaseButton(state, viewModel, childModifier, horizontalPadding = 0.dp)

            Spacer(Modifier.weight(UIConstant.halfWeight))
        }
    }
}

@Composable
private fun IconImage(
    state: PaywallState.Loaded.Legacy,
    childModifier: Modifier,
) {
    IconImage(
        uri = state.templateConfiguration.images.iconUri,
        maxWidth = Template2UIConstants.maxIconWidth,
        iconCornerRadius = Template2UIConstants.iconCornerRadius,
        childModifier = childModifier,
    )
}

@Composable
private fun Title(
    state: PaywallState.Loaded.Legacy,
    childModifier: Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Markdown(
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Black,
        textAlign = textAlign,
        text = state.selectedLocalization.title,
        color = state.templateConfiguration.getCurrentColors().text1,
        textFillMaxWidth = true,
        modifier = childModifier,
    )
}

@Composable
private fun Subtitle(
    state: PaywallState.Loaded.Legacy,
    childModifier: Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Markdown(
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Normal,
        textAlign = textAlign,
        text = state.selectedLocalization.subtitle ?: "",
        color = state.templateConfiguration.getCurrentColors().text1,
        textFillMaxWidth = true,
        modifier = childModifier,
    )
}

@Composable
private fun AnimatedPackages(
    state: PaywallState.Loaded.Legacy,
    packageSelectionVisible: Boolean,
    landscapeLayout: Boolean,
    viewModel: PaywallViewModel,
    childModifier: Modifier,
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
                    if (landscapeLayout) {
                        UIConstant.defaultVerticalSpacing / 2.0f
                    } else {
                        UIConstant.defaultVerticalSpacing
                    },
                    Alignment.CenterVertically,
                ),
            ) {
                state.templateConfiguration.packages.all.forEach { packageInfo ->
                    SelectPackageButton(state, packageInfo, viewModel, childModifier)
                }
            }
        }
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun ColumnScope.SelectPackageButton(
    state: PaywallState.Loaded.Legacy,
    packageInfo: TemplateConfiguration.PackageInfo,
    viewModel: PaywallViewModel,
    childModifier: Modifier,
) {
    val colors = state.templateConfiguration.getCurrentColors()
    val isSelected = packageInfo == state.selectedPackage.value

    val buttonAlpha = viewModel.packageButtonActionInProgressOpacityAnimation()
    val background = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.accent2,
        unselectedColor = colors.background,
    )
    val textColor = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.accent1,
        unselectedColor = colors.text1,
    )

    val border = if (isSelected) {
        null
    } else {
        BorderStroke(
            UIConstant.defaultPackageBorderWidth,
            colors.text1.copy(alpha = Template2UIConstants.fadedColorOpacity),
        )
    }
    Button(
        modifier = childModifier
            .fillMaxWidth()
            .alpha(buttonAlpha)
            .align(Alignment.Start)
            .semantics {
                selected = isSelected
            }
            .testTag(TestTag.selectButtonTestTag(packageInfo.rcPackage.identifier)),
        onClick = { viewModel.selectPackage(packageInfo) },
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = textColor),
        shape = RoundedCornerShape(UIConstant.defaultPackageCornerRadius),
        contentPadding = PaddingValues(
            vertical = UIConstant.defaultVerticalSpacing,
            horizontal = UIConstant.defaultHorizontalPadding,
        ),
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckmarkBox(isSelected = isSelected, colors = state.currentColors)
                Text(
                    text = packageInfo.localization.offerName ?: packageInfo.rcPackage.product.title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
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
            .size(Template2UIConstants.checkmarkSize)
            .clip(CircleShape)
            .background(colors.accent2.copy(alpha = Template2UIConstants.fadedColorOpacity)),
    ) {
        if (isSelected) {
            PaywallIcon(icon = PaywallIconName.CHECK_CIRCLE, tintColor = colors.accent1)
        }
    }
}

@Preview(showBackground = true, locale = "en-rUS", group = "full-screen", name = "Portrait")
@Preview(
    showBackground = true,
    locale = "en-rUS",
    group = "full-screen",
    name = "Landscape",
    widthDp = 720,
    heightDp = 380,
)
@Preview(showBackground = true, locale = "es-rES", group = "full-screen")
@Preview(showBackground = true, device = Devices.NEXUS_7, group = "full-screen")
@Preview(showBackground = true, device = Devices.NEXUS_10, group = "full-screen")
@Composable
private fun Template2PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template2Offering),
    )
}

@Preview(showBackground = true, locale = "en-rUS", group = "footer")
@Preview(showBackground = true, locale = "es-rES", group = "footer")
@Composable
private fun Template2PaywallFooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template2Offering),
    )
}

@Preview(showBackground = true, locale = "en-rUS", group = "condensed")
@Preview(showBackground = true, locale = "es-rES", group = "condensed")
@Composable
private fun Template2PaywallFooterCondensedPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template2Offering),
    )
}
