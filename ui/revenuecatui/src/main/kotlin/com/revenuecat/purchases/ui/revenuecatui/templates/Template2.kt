package com.revenuecat.purchases.ui.revenuecatui.templates

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation

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
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
) {
    Box {
        PaywallBackground(state.templateConfiguration)

        Column {
            Template2MainContent(state, viewModel, childModifier)
            PurchaseButton(state, viewModel, childModifier)
            Footer(
                templateConfiguration = state.templateConfiguration,
                viewModel = viewModel,
                childModifier = childModifier,
            )
        }
    }
}

@Composable
private fun ColumnScope.Template2MainContent(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    childModifier: Modifier,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .conditional(state.isInFullScreenMode) {
                Modifier.weight(1f)
            }
            .padding(horizontal = UIConstant.defaultHorizontalPadding, vertical = UIConstant.defaultVerticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        if (state.isInFullScreenMode) {
            Spacer(Modifier.weight(1f))

            IconImage(
                uri = state.templateConfiguration.images.iconUri,
                maxWidth = Template2UIConstants.maxIconWidth,
                iconCornerRadius = Template2UIConstants.iconCornerRadius,
                childModifier = childModifier,
            )
            val localizedConfig = state.selectedLocalization
            val colors = state.templateConfiguration.getCurrentColors()
            Markdown(
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                text = localizedConfig.title,
                color = colors.text1,
                modifier = childModifier,
            )
            Markdown(
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                text = localizedConfig.subtitle ?: "",
                color = colors.text1,
                modifier = childModifier,
            )

            Spacer(Modifier.weight(1f))
        }

        state.templateConfiguration.packages.all.forEach { packageInfo ->
            SelectPackageButton(state, packageInfo, viewModel, childModifier)
        }

        if (state.isInFullScreenMode) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun ColumnScope.SelectPackageButton(
    state: PaywallState.Loaded,
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
            .align(Alignment.Start),
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

@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
private fun Template2PaywallPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder()
            .setOffering(TestData.template2Offering)
            .build(),
    )
}

@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
private fun Template2PaywallFooterPreview() {
    val options = PaywallOptions.Builder()
        .setOffering(TestData.template2Offering)
        .build()
    options.mode = PaywallMode.FOOTER
    InternalPaywall(options)
}
