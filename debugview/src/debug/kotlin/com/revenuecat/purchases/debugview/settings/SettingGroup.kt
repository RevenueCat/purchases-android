package com.revenuecat.purchases.debugview.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.debugview.DebugRevenueCatViewModel
import com.revenuecat.purchases.debugview.findActivity
import com.revenuecat.purchases.debugview.models.SettingGroupState
import com.revenuecat.purchases.debugview.models.SettingScreenState
import com.revenuecat.purchases.debugview.models.SettingState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SettingGroup(
    settingGroupState: SettingGroupState,
    viewModel: DebugRevenueCatViewModel,
    activity: Activity = LocalContext.current.findActivity(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = settingGroupState.title,
            style = MaterialTheme.typography.h6,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(elevation = 4.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                settingGroupState.settings.forEach { settingState ->
                    when (settingState) {
                        is SettingState.Text -> SettingText(settingState)
                        is SettingState.OfferingSetting -> SettingOffering(
                            settingState,
                            activity = activity,
                            screenViewModel = viewModel,
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingGroupPreview() {
    val viewModel = object : DebugRevenueCatViewModel {
        override val state: StateFlow<SettingScreenState>
            get() = error("Not expected to be called")

        override fun toastDisplayed() {
            error("Not expected to be called")
        }

        override fun purchasePackage(activity: Activity, rcPackage: Package) {
            error("Not expected to be called")
        }

        override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
            error("Not expected to be called")
        }

        override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
            error("Not expected to be called")
        }
    }
    SettingGroup(
        SettingGroupState(
            "Settings group",
            listOf(
                SettingState.Text("Settings text 1", "Settings content 1"),
                SettingState.Text("Settings text 2", "Settings content 2"),
            ),
        ),
        viewModel,
    )
}
