package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.ConsistentPackageContentView
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonActionInProgressOpacityAnimation
import com.revenuecat.purchases.ui.revenuecatui.extensions.packageButtonColorAnimation

private object Template4UIConstants {
    val packageHorizontalSpacing = 8.dp
    val checkmarkSize = 18.dp
    const val fadedColorOpacity = 0.3f
}

@Composable
internal fun Template4(
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
    if (state.isInFullScreenMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                            bottomEnd = 0.dp
                        )
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
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
    val colors = state.templateConfiguration.getCurrentColors()

    Column(
        modifier = Modifier
            .padding(top = UIConstant.defaultVerticalSpacing),
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing)
    ) {
        if (state.isInFullScreenMode) {
            val localizedConfig = state.selectedLocalization
            Markdown(
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                text = localizedConfig.title,
                color = colors.text1,
                modifier = Modifier.padding(horizontal = UIConstant.defaultHorizontalPadding)
            )
        }

        Packages(state, viewModel)

        ConsistentPackageContentView(state = state) {
            IntroEligibilityStateView(
                textWithNoIntroOffer = it.localization.offerDetails,
                textWithIntroOffer = it.localization.offerDetailsWithIntroOffer,
                textWithMultipleIntroOffers = it.localization.offerDetailsWithMultipleIntroOffers,
                eligibility = it.introEligibility,
                color = colors.text1,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        PurchaseButton(state, viewModel)

        Footer(
            templateConfiguration = state.templateConfiguration,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun ColumnScope.Packages(
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
//            .conditional(state.isInFullScreenMode) {
//                Modifier.weight(1f)
//            }
            .padding(horizontal = UIConstant.defaultHorizontalPadding, vertical = UIConstant.defaultVerticalSpacing),
        horizontalArrangement = Arrangement.spacedBy(space = Template4UIConstants.packageHorizontalSpacing)
    ) {
        state.templateConfiguration.packages.all.forEach { packageInfo ->
            SelectPackageButton(state, packageInfo, viewModel)
        }
    }
}

@SuppressWarnings("LongMethod")
@Composable
private fun RowScope.SelectPackageButton(
    state: PaywallViewState.Loaded,
    packageInfo: TemplateConfiguration.PackageInfo,
    viewModel: PaywallViewModel,
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
            colors.text1.copy(alpha = Template4UIConstants.fadedColorOpacity),
        )
    }
    Button(
        modifier = Modifier
            .alpha(buttonAlpha),
        onClick = { viewModel.selectPackage(packageInfo) },
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = textColor),
        shape = RoundedCornerShape(UIConstant.defaultCornerRadius),
        contentPadding = PaddingValues(
            vertical = UIConstant.defaultVerticalSpacing,
            horizontal = UIConstant.defaultHorizontalPadding,
        ),
        border = border,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
//                CheckmarkBox(isSelected = isSelected, colors = state.currentColors)
                Text(
                    text = packageInfo.localization.offerName ?: packageInfo.rcPackage.product.title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = packageInfo.rcPackage.product.price.formatted,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
//            }
        }
    }
}

@Composable
private fun CheckmarkBox(isSelected: Boolean, colors: TemplateConfiguration.Colors) {
    Box(
        modifier = Modifier
            .size(Template4UIConstants.checkmarkSize)
            .clip(CircleShape)
            .background(colors.accent2.copy(alpha = Template4UIConstants.fadedColorOpacity)),
    ) {
        if (isSelected) {
            PaywallIcon(icon = PaywallIconName.CHECK_CIRCLE, tintColor = colors.accent1)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Template4PaywallPreview() {
    InternalPaywallView(
        options = PaywallViewOptions.Builder()
            .setOffering(TestData.template4Offering)
            .build(),
    )
}
