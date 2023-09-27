package com.revenuecat.purchases.ui.revenuecatui.templates

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

private object Template3UIConstants {
    val maxIconWidth = 140.dp
    val iconCornerRadius = 16.dp
}

@Composable
internal fun Template3(
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
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
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.CenterVertically),
    ) {
        IconImage(state.templateConfiguration.images.iconUri)
    }
}

@Composable
private fun IconImage(iconUri: Uri?) {
    iconUri?.let { uri ->
        val modifier = Modifier
            .aspectRatio(ratio = 1f)
            .widthIn(max = Template3UIConstants.maxIconWidth)
            .clip(RoundedCornerShape(Template3UIConstants.iconCornerRadius))
        if (isInPreviewMode()) {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .size(100.dp),
            )
        } else {
            RemoteImage(
                urlString = uri.toString(),
                modifier = modifier,
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
