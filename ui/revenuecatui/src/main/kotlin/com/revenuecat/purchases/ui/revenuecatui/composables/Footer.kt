package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ColorsFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import java.net.URL

@Composable
internal fun Footer(
    templateConfiguration: TemplateConfiguration,
    viewModel: PaywallViewModel
) {
    Footer(
        configuration = templateConfiguration.configuration,
        colors = templateConfiguration.getCurrentColors(),
        viewModel = viewModel
    )
}

@Composable
internal fun Footer(
    configuration: PaywallData.Configuration,
    colors: TemplateConfiguration.Colors,
    viewModel: PaywallViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Max)
            .padding(
                start = UIConstant.defaultHorizontalPadding,
                end = UIConstant.defaultHorizontalPadding,
                bottom = UIConstant.defaultButtonVerticalSpacing,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = colors.text1

        if (configuration.displayRestorePurchases) {
            Button(
                text = stringResource(id = R.string.restore_purchases),
                color = color,
                action = { viewModel.restorePurchases() }
            )

            if (configuration.termsOfServiceURL != null || configuration.privacyURL != null) {
                Separator(color = color)
            }
        }

        configuration.termsOfServiceURL?.let {
            Button(
                text = stringResource(id = R.string.terms_and_conditions),
                color = color,
                action = { viewModel.openURL(it) }
            )

            if (configuration.privacyURL != null) {
                Separator(color = color)
            }
        }

        configuration.privacyURL?.let {
            Button(
                text = stringResource(id = R.string.privacy_policy),
                color = color,
                action = { viewModel.openURL(it) }
            )
        }
    }
}

@Composable
private fun Separator(color: Color) {
    Canvas(
        modifier = Modifier
            .size(10.dp),
        onDraw = {
            drawCircle(color = color)
        }
    )
}

@Composable
private fun RowScope.Button(
    text: String,
    color: Color,
    action: () -> Unit
) {
    Column(modifier = Modifier.weight(1f)) {
        TextButton(onClick = {
            action.invoke()
        }) {
            Text(
                text = text,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FooterPreview() {
    Footer(
        configuration = PaywallData.Configuration(
            packages = listOf(),
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),
            displayRestorePurchases = true,
            images = PaywallData.Configuration.Images(),
            blurredBackgroundImage = false,
            colors = TestData.template2.config.colors,
            defaultPackage = null,
        ),
        colors = ColorsFactory.create(paywallDataColors = TestData.template2.config.colors.light),
        viewModel = MockViewModel(TestData.template2Offering)
    )
}
