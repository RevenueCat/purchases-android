package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Composable
internal fun PurchaseButton(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
) {
    DisableTouchesComposable(shouldDisable = viewModel.actionInProgress.value) {
        PurchaseButton(
            colors = state.templateConfiguration.getCurrentColors(),
            packages = state.templateConfiguration.packages,
            selectedPackage = state.selectedPackage,
            viewModel = viewModel,
            childModifier = childModifier,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun PurchaseButton(
    colors: TemplateConfiguration.Colors,
    packages: TemplateConfiguration.PackageConfiguration,
    selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
    viewModel: PaywallViewModel,
    childModifier: Modifier = Modifier,
) {
    Column(
        modifier = childModifier
            .fillMaxWidth()
            .padding(horizontal = UIConstant.defaultHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current

        val labelOpacity by animateFloatAsState(
            targetValue = if (viewModel.actionInProgress.value) 0.0f else 1.0f,
            animationSpec = UIConstant.defaultAnimation(),
            label = "PurchaseButton.label",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    val p = selectedPackage.value
                    text = AnnotatedString(
                        introEligibilityText(
                            eligibility = p.introEligibility,
                            textWithIntroOffer = p.localization.callToActionWithIntroOffer,
                            textWithMultipleIntroOffers = p.localization.callToActionWithMultipleIntroOffers,
                            textWithNoIntroOffer = p.localization.callToAction,
                        ),
                    )
                }
                .background(
                    brush = buttonBrush(colors),
                    shape = ButtonDefaults.shape,
                ),
            onClick = { viewModel.purchaseSelectedPackage(context) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // color set on background
                contentColor = colors.callToActionForeground,
            ),
        ) {
            Box(
                // Ignore children as accessibility elements
                modifier = Modifier.clearAndSetSemantics {},
            ) {
                ConsistentPackageContentView(
                    packages = packages.all,
                    selected = selectedPackage.value,
                ) {
                    val localization = it.localization
                    IntroEligibilityStateView(
                        textWithNoIntroOffer = localization.callToAction,
                        textWithIntroOffer = localization.callToActionWithIntroOffer,
                        textWithMultipleIntroOffers = localization.callToActionWithMultipleIntroOffers,
                        eligibility = it.introEligibility,
                        color = colors.callToActionForeground,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(vertical = UIConstant.defaultVerticalSpacing / 3)
                            .alpha(labelOpacity),
                    )
                }

                Box(
                    // Ensure that the spinner takes the same size as the label
                    modifier = Modifier.matchParentSize(),
                ) {
                    LoadingSpinner(shouldShow = viewModel.actionInProgress.value, colors = colors)
                }
            }
        }
    }
}

@ReadOnlyComposable
@Composable
private fun buttonBrush(colors: TemplateConfiguration.Colors): Brush {
    return colors.callToActionSecondaryBackground?.let { secondaryColor ->
        Brush.verticalGradient(
            listOf(
                colors.callToActionBackground,
                secondaryColor,
            ),
        )
    } ?: SolidColor(colors.callToActionBackground)
}

@Composable
private fun BoxScope.LoadingSpinner(shouldShow: Boolean, colors: TemplateConfiguration.Colors) {
    AnimatedVisibility(
        visible = shouldShow,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(animationSpec = UIConstant.defaultAnimation()),
        exit = fadeOut(animationSpec = UIConstant.defaultAnimation()),
        label = "PurchaseButton.LoadingSpinner",
    ) {
        CircularProgressIndicator(
            color = colors.callToActionForeground,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PurchaseButtonPreview() {
    val viewModel = MockViewModel(offering = TestData.template2Offering, allowsPurchases = true)
    val state = viewModel.loadedState()

    if (state != null) {
        PurchaseButton(
            state = state,
            viewModel = viewModel,
        )
    }
}
