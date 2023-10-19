package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ColorsFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import java.net.URL

@Composable
internal fun Footer(
    templateConfiguration: TemplateConfiguration,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
    allPlansTapped: (() -> Unit)? = null,
) {
    Footer(
        mode = templateConfiguration.mode,
        configuration = templateConfiguration.configuration,
        colors = templateConfiguration.getCurrentColors(),
        viewModel = viewModel,
        childModifier = childModifier,
        allPlansTapped = allPlansTapped,
    )
}

@Suppress("LongParameterList")
@Composable
private fun Footer(
    mode: PaywallMode,
    configuration: PaywallData.Configuration,
    colors: TemplateConfiguration.Colors,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
    allPlansTapped: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(horizontal = UIConstant.defaultHorizontalPadding)
            .padding(bottom = UIConstant.defaultVerticalSpacing),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = colors.text1

        if (mode == PaywallMode.FOOTER_CONDENSED && allPlansTapped != null) {
            Button(
                color = color,
                childModifier = childModifier,
                R.string.all_plans,
                action = allPlansTapped,
            )

            if (configuration.displayRestorePurchases ||
                configuration.termsOfServiceURL != null ||
                configuration.privacyURL != null
            ) {
                Separator(color = color)
            }
        }

        if (configuration.displayRestorePurchases) {
            Button(
                color = color,
                childModifier = childModifier,
                R.string.restore_purchases,
                R.string.restore,
            ) { viewModel.restorePurchases() }

            if (configuration.termsOfServiceURL != null || configuration.privacyURL != null) {
                Separator(color = color)
            }
        }

        configuration.termsOfServiceURL?.let {
            Button(
                color = color,
                childModifier = childModifier,
                R.string.terms_and_conditions,
                R.string.terms,
            ) { viewModel.openURL(it, context) }

            if (configuration.privacyURL != null) {
                Separator(color = color)
            }
        }

        configuration.privacyURL?.let {
            Button(
                color = color,
                childModifier = childModifier,
                R.string.privacy_policy,
                R.string.privacy,
            ) { viewModel.openURL(it, context) }
        }
    }
}

@Composable
private fun RowScope.Separator(color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .weight(1.0f / 2),
    ) {
        Box(
            modifier = Modifier
                // TODO-Paywalls: scale based on font size
                .size(5.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun RowScope.Button(
    color: Color,
    childModifier: Modifier,
    @StringRes vararg texts: Int,
    action: () -> Unit,
) {
    fun <T> merge(first: List<T>, second: List<T>): List<T> {
        return first + second
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
    ) {
        TextButton(
            onClick = action::invoke,
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.align(CenterHorizontally),
        ) {
            // Find the first view that fits, starting from the longest text and fitting in one line,
            // ending with the shortest in multiple lines.
            AdaptiveComposable(
                merge(
                    texts.map {
                        {
                            Text(
                                text = stringResource(it),
                                color = color,
                                textAlign = TextAlign.Center,
                                style = FooterConstants.style(),
                                softWrap = false,
                                maxLines = 1,
                                modifier = childModifier,
                            )
                        }
                    },
                    texts.map {
                        {
                            Text(
                                text = stringResource(it),
                                color = color,
                                textAlign = TextAlign.Center,
                                style = FooterConstants.style(),
                                softWrap = true,
                                modifier = childModifier,
                            )
                        }
                    },
                ),
            )
        }
    }
}

private object FooterConstants {
    @ReadOnlyComposable @Composable
    fun style(): TextStyle = MaterialTheme.typography.bodySmall
}

@Preview(showBackground = true)
@Composable
private fun FooterPreview() {
    Footer(
        mode = PaywallMode.FULL_SCREEN,
        configuration = PaywallData.Configuration(
            packageIds = listOf(),
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),
            displayRestorePurchases = true,
            images = PaywallData.Configuration.Images(),
            blurredBackgroundImage = false,
            colors = TestData.template2.config.colors,
            defaultPackage = null,
        ),
        colors = ColorsFactory.create(paywallDataColors = TestData.template2.config.colors.light),
        viewModel = MockViewModel(PaywallMode.FULL_SCREEN, TestData.template2Offering),
    )
}
