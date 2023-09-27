package com.revenuecat.purchases.ui.revenuecatui.templates

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

private object Template3UIConstants {
    val maxIconWidth = 140.dp
    val iconCornerRadius = 16.dp
    val iconSize = 35.dp
    val iconPadding = 5.dp
}

@Composable
internal fun Template3(
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Template3MainContent(state, viewModel)
        Spacer(modifier = Modifier.weight(1f))
        PurchaseButton(state, viewModel)
        Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
    }
}

@Composable
private fun Template3MainContent(state: PaywallViewState.Loaded, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = UIConstant.defaultHorizontalPadding,
                vertical = UIConstant.defaultVerticalSpacing,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        IconImage(state.templateConfiguration.images.iconUri)
        val localizedConfig = state.selectedLocalization
        val colors = state.templateConfiguration.getCurrentColors()
        Text(
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            text = localizedConfig.title,
            color = colors.text1,
        )
        Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing * 2))
        Features(
            features = localizedConfig.features,
            colors = colors,
        )
    }
}

@Composable
private fun Features(
    features: List<PaywallData.LocalizedConfiguration.Feature>,
    colors: TemplateConfiguration.Colors,
) {
    if (features.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.Top),
    ) {
        features.forEach { feature ->
            Feature(
                feature = feature,
                colors = colors,
            )
        }
    }
}

@Composable
private fun Feature(
    feature: PaywallData.LocalizedConfiguration.Feature,
    colors: TemplateConfiguration.Colors,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
    ) {
        feature.iconID?.let { PaywallIconName.fromValue(it) }?.let { icon ->
            Box(
                modifier = Modifier
                    .size(Template3UIConstants.iconSize)
                    .clip(CircleShape)
                    .background(colors.accent2),
            ) {
                PaywallIcon(
                    icon = icon,
                    tintColor = colors.accent1,
                    modifier = Modifier.padding(Template3UIConstants.iconPadding),
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = UIConstant.defaultHorizontalPadding),
        ) {
            Text(
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                text = feature.title,
                color = colors.text1,
            )
            feature.content?.let { content ->
                Text(
                    modifier = Modifier.padding(top = Template3UIConstants.iconPadding * 2),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    text = content,
                    color = colors.text1,
                )
            }
        }
    }
}

@Composable
private fun IconImage(iconUri: Uri?) {
    iconUri?.let { uri ->
        val modifier = Modifier
            .clip(RoundedCornerShape(Template3UIConstants.iconCornerRadius))
        if (isInPreviewMode()) {
            Box(
                modifier = modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .size(100.dp),
            )
        } else {
            RemoteImage(
                urlString = uri.toString(),
                modifier = modifier
                    .aspectRatio(ratio = 1f)
                    .widthIn(max = Template3UIConstants.maxIconWidth),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Preview
@Composable
private fun Template3Preview() {
    InternalPaywallView(offering = TestData.template3Offering)
}
