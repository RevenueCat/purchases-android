@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.AutoResizedText
import com.revenuecat.purchases.ui.revenuecatui.composables.ConsistentPackageContentView
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.localizedDiscount
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation
import kotlin.math.min

private object Template4UIConstants {
    val packageButtonContentVerticalSpacing = 4.dp
    val packageHorizontalSpacing = 8.dp
    val packagesHorizontalPadding = 24.dp
    val checkmarkSize = 18.dp
    val checkmarkPadding = 8.dp
    val discountVerticalPadding = 4.dp
    const val maxPackagesToDisplay = 3f
}

@Composable
internal fun Template4(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    if (state.isInFullScreenMode) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            PaywallBackground(state.templateConfiguration)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(
                        RoundedCornerShape(
                            topStart = UIConstant.defaultCornerRadius,
                            topEnd = UIConstant.defaultCornerRadius,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                        ),
                    )
                    .background(state.currentColors.background),
            ) {
                Template4MainContent(state, viewModel)
            }
        }
    } else {
        Template4MainContent(state, viewModel)
    }
}

@Composable
private fun Template4MainContent(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    val colors = state.templateConfiguration.getCurrentColors()

    Column(
        modifier = Modifier
            .padding(top = UIConstant.defaultVerticalSpacing),
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isInFullScreenMode) {
            val localizedConfig = state.selectedLocalization
            Markdown(
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                text = localizedConfig.title,
                color = colors.text1,
                modifier = Modifier.padding(horizontal = UIConstant.defaultHorizontalPadding),
            )
        }

        var packageSelectorVisible by remember {
            mutableStateOf(state.templateConfiguration.mode != PaywallMode.FOOTER_CONDENSED)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visible = packageSelectorVisible,
                enter = expandVertically(expandFrom = Alignment.Bottom),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
                label = "SelectPackagesVisibility",
            ) {
                Packages(state, viewModel)
            }

            AnimatedVisibility(visible = packageSelectorVisible) {
                Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))
            }

            ConsistentPackageContentView(state = state) {
                IntroEligibilityStateView(
                    textWithNoIntroOffer = it.localization.offerDetails,
                    textWithIntroOffer = it.localization.offerDetailsWithIntroOffer,
                    textWithMultipleIntroOffers = it.localization.offerDetailsWithMultipleIntroOffers,
                    eligibility = it.introEligibility,
                    color = colors.text1,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        PurchaseButton(state, viewModel)

        Footer(
            templateConfiguration = state.templateConfiguration,
            viewModel = viewModel,
            allPlansTapped = { packageSelectorVisible = !packageSelectorVisible },
        )
    }
}

@Composable
private fun Packages(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    @SuppressWarnings("MagicNumber")
    fun packagesToDisplay(numberOfPackages: Float): Float {
        // TODO-PAYWALLS: Implement different counts for different screen sizes
        val desiredCount = Template4UIConstants.maxPackagesToDisplay + 0.5f
        return min(min(desiredCount, numberOfPackages), Template4UIConstants.maxPackagesToDisplay)
    }

    fun BoxWithConstraintsScope.packageWidth(numberOfPackages: Float): Dp {
        val packages = packagesToDisplay(numberOfPackages)
        val totalPadding = Template4UIConstants.packagesHorizontalPadding * 2
        val totalSpaceBetweenPackages = Template4UIConstants.packageHorizontalSpacing * (packages - 1)
        val availableWidth = maxWidth - totalPadding - totalSpaceBetweenPackages
        return availableWidth / packages
    }

    BoxWithConstraints {
        val numberOfPackages = state.templateConfiguration.packages.all.size
        val packageWidth = packageWidth(numberOfPackages.toFloat())
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = Template4UIConstants.packagesHorizontalPadding,
                    vertical = UIConstant.defaultVerticalSpacing,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                space = Template4UIConstants.packageHorizontalSpacing,
            ),
        ) {
            state.templateConfiguration.packages.all.forEach { packageInfo ->
                SelectPackageButton(
                    state,
                    packageInfo,
                    viewModel,
                    Modifier.width(packageWidth),
                )
            }
        }
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun SelectPackageButton(
    state: PaywallState.Loaded,
    packageInfo: TemplateConfiguration.PackageInfo,
    viewModel: PaywallViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = state.templateConfiguration.getCurrentColors()
    val isSelected = packageInfo == state.selectedPackage.value

    val buttonAlpha = viewModel.packageButtonActionInProgressOpacityAnimation()
    val mainColor = state.packageButtonColorAnimation(
        packageInfo = packageInfo,
        selectedColor = colors.accent1,
        unselectedColor = colors.accent2,
    )

    var columnModifier = modifier.clip(RoundedCornerShape(UIConstant.defaultCornerRadius))

    val discountText = packageInfo.localizedDiscount(viewModel.resourceProvider)

    if (packageInfo.discountRelativeToMostExpensivePerMonth != null) {
        columnModifier = columnModifier.background(mainColor)
    }

    Column(
        modifier = columnModifier
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DiscountRelativeToMostExpensivePerMonth(colors = colors, text = discountText, selected = isSelected)

        Box {
            Button(
                modifier = modifier
                    .alpha(buttonAlpha)
                    .semantics {
                        selected = isSelected
                        // Append discount
                        discountText?.let {
                            text = AnnotatedString(it)
                        }
                    }
                    .fillMaxHeight(),
                onClick = { viewModel.selectPackage(packageInfo) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.background),
                shape = RoundedCornerShape(UIConstant.defaultCornerRadius),
                contentPadding = PaddingValues(
                    vertical = UIConstant.defaultVerticalSpacing,
                    horizontal = UIConstant.defaultHorizontalPadding,
                ),
                border = BorderStroke(
                    UIConstant.defaultPackageBorderWidth,
                    mainColor,
                ),
            ) {
                SelectPackageButtonContent(packageInfo, colors)
            }

            CheckmarkBox(
                isSelected = isSelected,
                colors = state.currentColors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Template4UIConstants.checkmarkPadding),
            )
        }
    }
}

@Composable
private fun SelectPackageButtonContent(
    packageInfo: TemplateConfiguration.PackageInfo,
    colors: TemplateConfiguration.Colors,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Template4UIConstants.packageButtonContentVerticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OfferName(
            packageInfo = packageInfo,
            textColor = colors.text1,
            modifier = Modifier
                .wrapContentSize()
                .weight(1f),
        )

        Text(
            text = packageInfo.rcPackage.product.price.formatted,
            color = colors.text1,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DiscountRelativeToMostExpensivePerMonth(
    text: String?,
    colors: TemplateConfiguration.Colors,
    selected: Boolean,
) {
    AutoResizedText(
        text = text?.uppercase() ?: "",
        color = if (selected) colors.text2 else colors.text3,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(
                horizontal = UIConstant.defaultHorizontalPadding,
                vertical = Template4UIConstants.discountVerticalPadding,
            )
            .clearAndSetSemantics {},
    )
}

@Composable
private fun OfferName(
    packageInfo: TemplateConfiguration.PackageInfo,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    var firstRow: String? = null
    var secondRow: String = packageInfo.rcPackage.product.title

    packageInfo.localization.offerName?.let { offerName ->
        val components = offerName.split(" ", limit = 2)
        if (components.size == 2) {
            firstRow = components[0]
            secondRow = components[1]
        } else {
            secondRow = offerName
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        firstRow?.let {
            Text(
                text = it,
                color = textColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = secondRow,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CheckmarkBox(
    isSelected: Boolean,
    colors: TemplateConfiguration.Colors,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Template4UIConstants.checkmarkSize)
            .clip(CircleShape),
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = UIConstant.defaultAnimation()),
            exit = fadeOut(animationSpec = UIConstant.defaultAnimation()),
        ) {
            PaywallIcon(icon = PaywallIconName.CHECK_CIRCLE, tintColor = colors.accent1)
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template4PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template4Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template4PaywallFooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template4Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template4PaywallFooterCondensedPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template4Offering),
    )
}
