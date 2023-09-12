package com.revenuecat.purchases.ui.revenuecatui.templates.template2

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.accent1Color
import com.revenuecat.purchases.ui.revenuecatui.accent2Color
import com.revenuecat.purchases.ui.revenuecatui.backgroundColor
import com.revenuecat.purchases.ui.revenuecatui.callToActionBackgroundColor
import com.revenuecat.purchases.ui.revenuecatui.callToActionForegroundColor
import com.revenuecat.purchases.ui.revenuecatui.getActivity
import com.revenuecat.purchases.ui.revenuecatui.getColors
import com.revenuecat.purchases.ui.revenuecatui.localizedConfig
import com.revenuecat.purchases.ui.revenuecatui.text1Color

@Composable
internal fun Template2(state: PaywallViewState.Template2, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Template2MainContent(state, viewModel)
        Spacer(modifier = Modifier.weight(1f))
        PurchaseButton(state, viewModel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = UIConstant.defaultHorizontalPadding,
                    end = UIConstant.defaultHorizontalPadding,
                    bottom = UIConstant.defaultButtonVerticalSpacing,
                ),
            horizontalArrangement = Arrangement.Center,
        ) {
            RestorePurchasesButton(viewModel)
        }
    }
}

@Composable
private fun Template2MainContent(state: PaywallViewState.Template2, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstant.defaultButtonVerticalSpacing, Alignment.CenterVertically),
    ) {
        // TODO-PAYWALLS: Replace with correct image
        val drawable = LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.packageName)
        Image(drawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(), contentDescription = "")

        val localizedConfig = state.paywallData.localizedConfig()
        Text(
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            text = localizedConfig?.title ?: "",
            color = state.paywallData.getColors().text1Color,
        )
        Text(
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            text = localizedConfig?.subtitle ?: "",
            color = state.paywallData.getColors().text1Color,
        )
        state.packages.forEach { aPackage ->
            SelectPackageButton(state, aPackage, viewModel)
        }
    }
}

@Composable
private fun PurchaseButton(state: PaywallViewState.Template2, viewModel: PaywallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val activity = LocalContext.current.getActivity() ?: error("Error finding activity")
        val colors = state.paywallData.getColors()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.purchaseSelectedPackage(activity) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.callToActionBackgroundColor,
                contentColor = colors.callToActionForegroundColor,
            ),
        ) {
            Text(text = state.paywallData.localizedConfig()?.callToAction ?: "")
        }
    }
}

@Composable
private fun RestorePurchasesButton(viewModel: PaywallViewModel) {
    TextButton(onClick = { viewModel.restorePurchases() }) {
        Text(text = stringResource(id = R.string.restore_purchases))
    }
}

@Composable
private fun SelectPackageButton(
    state: PaywallViewState.Template2,
    aPackage: Package,
    viewModel: PaywallViewModel,
) {
    val colors = state.paywallData.getColors()
    val isSelected = aPackage == state.selectedPackage
    val (background, textColor) = if (isSelected) {
        colors.accent2Color to colors.accent1Color
    } else {
        // TODO-PAYWALLS: Find correct background unselected color
        colors.backgroundColor to colors.text1Color
    }
    val border = if (isSelected) null else BorderStroke(2.dp, colors.text1Color)
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { viewModel.selectPackage(aPackage) },
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = textColor),
        shape = RoundedCornerShape(15.dp),
        border = border,
    ) {
        Text(text = "Purchase ${aPackage.identifier}. Price: ${aPackage.product.price.formatted}")
    }
}
