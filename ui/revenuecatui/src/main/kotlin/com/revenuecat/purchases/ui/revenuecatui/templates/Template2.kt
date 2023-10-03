package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IconImage
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroEligibilityStateView
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.currentColors
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

private object Template2UIConstants {
    val maxIconWidth = 140.dp
    val iconCornerRadius = 16.dp
    val checkmarkSize = 18.dp
    const val checkmarkUnselectedAlpha = 0.3f
}

@Composable
internal fun Template2(state: PaywallViewState.Loaded, viewModel: PaywallViewModel) {
    Box {
        PaywallBackground(state.templateConfiguration)

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = UIConstant.defaultHorizontalPadding,
                        vertical = UIConstant.defaultVerticalSpacing,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Template2MainContent(state, viewModel)
            }
            PurchaseButton(state, viewModel)
            Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
        }
    }
}

@Composable
private fun Template2MainContent(state: PaywallViewState.Loaded, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        IconImage(
            uri = state.templateConfiguration.images.iconUri,
            maxWidth = Template2UIConstants.maxIconWidth,
            iconCornerRadius = Template2UIConstants.iconCornerRadius,
        )
        val localizedConfig = state.selectedLocalization
        val colors = state.templateConfiguration.getCurrentColors()
        Text(
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            text = localizedConfig.title,
            color = colors.text1,
        )
        Text(
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            text = localizedConfig.subtitle ?: "",
            color = colors.text1,
        )
        state.templateConfiguration.packages.all.forEach { packageInfo ->
            SelectPackageButton(state, packageInfo, viewModel)
        }
    }
}

@Composable
private fun SelectPackageButton(
    state: PaywallViewState.Loaded,
    packageInfo: TemplateConfiguration.PackageInfo,
    viewModel: PaywallViewModel,
) {
    val colors = state.templateConfiguration.getCurrentColors()
    val isSelected = packageInfo == state.selectedPackage
    val (background, textColor) = if (isSelected) {
        colors.accent2 to colors.accent1
    } else {
        // TODO-PAYWALLS: Find correct background unselected color
        colors.background to colors.text1
    }
    val border = if (isSelected) null else BorderStroke(2.dp, colors.text1)
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { viewModel.selectPackage(packageInfo) },
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = textColor),
        shape = RoundedCornerShape(15.dp),
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckmarkBox(isSelected = isSelected, colors = state.currentColors)
                Text(
                    text = packageInfo.localization.offerName ?: packageInfo.rcPackage.product.title,
                )
            }
            IntroEligibilityStateView(
                textWithNoIntroOffer = packageInfo.localization.offerDetails,
                textWithIntroOffer = packageInfo.localization.offerDetailsWithIntroOffer,
                eligibility = packageInfo.rcPackage.introEligibility,
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
            .background(colors.accent2.copy(alpha = Template2UIConstants.checkmarkUnselectedAlpha)),
    ) {
        if (isSelected) {
            PaywallIcon(icon = PaywallIconName.CHECK_CIRCLE, tintColor = colors.accent1)
        }
    }
}

@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
internal fun Template2PaywallPreview() {
    InternalPaywallView(offering = TestData.template2Offering)
}
