package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Composable
internal fun PurchaseButton(
    state: PaywallViewState.Loaded,
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
        val colors = state.templateConfiguration.getCurrentColors()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.purchaseSelectedPackage(context) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.callToActionBackground,
                contentColor = colors.callToActionForeground,
            ),
        ) {
            IntroEligibilityStateView(
                textWithNoIntroOffer = state.selectedLocalization.callToAction,
                textWithIntroOffer = state.selectedLocalization.callToActionWithIntroOffer,
                eligibility = state.selectedPackage.introEligibility,
                color = colors.callToActionForeground,
            )
        }
    }
}
