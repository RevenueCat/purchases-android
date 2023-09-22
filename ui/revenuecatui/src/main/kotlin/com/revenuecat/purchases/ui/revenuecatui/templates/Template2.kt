package com.revenuecat.purchases.ui.revenuecatui.templates

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallBackground
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultAppIconPlaceholder

private object Template2UIConstants {
    val maxIconWidth = 140.dp
    val iconCornerRadius = 16.dp
}

@Composable
internal fun Template2(state: PaywallViewState.Loaded, viewModel: PaywallViewModel) {
    Box {
        PaywallBackground(state.templateConfiguration)

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Template2MainContent(state, viewModel)
            Spacer(modifier = Modifier.weight(1f))
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
        IconImage(state.templateConfiguration.images.iconUri)
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
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
private fun IconImage(uri: Uri?) {
    uri?.let {
        Column(modifier = Modifier.widthIn(max = Template2UIConstants.maxIconWidth)) {
            val modifier = Modifier
                .aspectRatio(ratio = 1f)
                .widthIn(max = Template2UIConstants.maxIconWidth)
                .clip(RoundedCornerShape(Template2UIConstants.iconCornerRadius))
            if (uri.toString().contains(PaywallData.defaultAppIconPlaceholder)) {
                AppIcon(
                    modifier = modifier,
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
}

@Composable
private fun AppIcon(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appIconResId = remember {
        val packageManager = context.packageManager
        context.applicationInfo.loadIcon(packageManager)
    }

    Image(
        bitmap = appIconResId.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
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
        Text(
            text = "Purchase ${packageInfo.rcPackage.identifier}. " +
                "Price: ${packageInfo.rcPackage.product.price.formatted}",
        )
    }
}

@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
internal fun Template2PaywallPreview() {
    InternalPaywallView(offering = TestData.template2Offering)
}
