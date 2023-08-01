package com.revenuecat.purchases.debugview.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.debugview.DebugRevenueCatViewModel
import com.revenuecat.purchases.debugview.findActivity
import com.revenuecat.purchases.debugview.models.InternalDebugRevenueCatScreenViewModel
import com.revenuecat.purchases.debugview.models.SettingScreenState
import com.revenuecat.purchases.debugview.models.SettingState
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SettingOffering(
    settingState: SettingState.OfferingSetting,
    activity: Activity = LocalContext.current.findActivity(),
    screenViewModel: DebugRevenueCatViewModel = viewModel<InternalDebugRevenueCatScreenViewModel>(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text = settingState.title, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.size(8.dp))
        settingState.offering.availablePackages.forEach { rcPackage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = BorderStroke(2.dp, Color.Gray),
            ) {
                SettingPackage(rcPackage, activity, screenViewModel)
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
internal fun SettingPackage(
    rcPackage: Package,
    activity: Activity = LocalContext.current.findActivity(),
    screenViewModel: DebugRevenueCatViewModel = viewModel<InternalDebugRevenueCatScreenViewModel>(),
) {
    val isSubscription = rcPackage.product.type == ProductType.SUBS
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Package ID:", style = MaterialTheme.typography.body1)
                Text(text = rcPackage.identifier, style = MaterialTheme.typography.body2)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Product ID:", style = MaterialTheme.typography.body1)
                Text(text = rcPackage.product.id, style = MaterialTheme.typography.body2)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Product Type:", style = MaterialTheme.typography.body1)
                Text(text = rcPackage.product.type.toString(), style = MaterialTheme.typography.body2)
            }
            if (!isSubscription) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Price:", style = MaterialTheme.typography.body1)
                    Text(
                        text = rcPackage.product.price.formatted,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
            Button(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { screenViewModel.purchasePackage(activity, rcPackage) },
            ) {
                Text(text = "Buy package", style = MaterialTheme.typography.subtitle2)
            }
            Button(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { screenViewModel.purchaseProduct(activity, rcPackage.product) },
            ) {
                Text(text = "Buy product", style = MaterialTheme.typography.subtitle2)
            }
        }
        if (isSubscription) {
            Divider()
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Subscription Options",
                style = MaterialTheme.typography.subtitle2,
            )
            rcPackage.product.subscriptionOptions?.forEach { subscriptionOption ->
                SettingSubscriptionOption(
                    activity = activity,
                    screenViewModel = screenViewModel,
                    subscriptionOption = subscriptionOption,
                    isDefaultOption = subscriptionOption == rcPackage.product.defaultOption,
                )
                if (subscriptionOption != rcPackage.product.subscriptionOptions?.last()) {
                    Divider()
                }
            }
        }
    }
}

@Composable
internal fun SettingSubscriptionOption(
    activity: Activity,
    screenViewModel: DebugRevenueCatViewModel,
    subscriptionOption: SubscriptionOption,
    isDefaultOption: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Subscription Option ID:", style = MaterialTheme.typography.body1)
            Text(text = subscriptionOption.id, style = MaterialTheme.typography.body2)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Tags:", style = MaterialTheme.typography.body1)
            Text(
                text = subscriptionOption.tags.takeIf { it.isNotEmpty() }?.toString() ?: "None",
                style = MaterialTheme.typography.body2,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Pricing phases:", style = MaterialTheme.typography.body1)
            val pricingPhasesString = subscriptionOption.pricingPhases.joinToString("\n") {
                "- ${it.price.formatted}/${it.billingPeriod.iso8601}"
            }
            Text(
                text = pricingPhasesString,
                style = MaterialTheme.typography.body2,
            )
        }
        Button(onClick = { screenViewModel.purchaseSubscriptionOption(activity, subscriptionOption) }) {
            val buttonText = if (isDefaultOption) {
                "Buy option (default)"
            } else {
                "Buy option"
            }
            Text(text = buttonText, style = MaterialTheme.typography.subtitle2)
        }
    }
}

@Suppress("MagicNumber")
private val offering: Offering
    get() {
        val price = Price("$9.99", 9990000, "USD")
        val purchasingData = object : PurchasingData {
            override val productId: String
                get() = "product_id"
            override val productType: ProductType
                get() = ProductType.SUBS
        }
        val subscriptionOption = object : SubscriptionOption {
            override val id: String
                get() = "monthly:free_trial"
            override val pricingPhases: List<PricingPhase>
                get() = listOf(
                    PricingPhase(
                        billingPeriod = Period(1, Period.Unit.MONTH, "P1M"),
                        recurrenceMode = RecurrenceMode.NON_RECURRING,
                        billingCycleCount = 0,
                        price = Price("$0", 0, "USD"),
                    ),
                    PricingPhase(
                        billingPeriod = Period(1, Period.Unit.MONTH, "P1M"),
                        recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                        billingCycleCount = 0,
                        price = price,
                    ),
                )
            override val tags: List<String>
                get() = listOf("tag1", "tag2")
            override val presentedOfferingIdentifier: String
                get() = "offering_id"
            override val purchasingData: PurchasingData
                get() = purchasingData
        }
        val subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption))
        val storeProduct = object : StoreProduct {
            override val id: String
                get() = "product_id"
            override val type: ProductType
                get() = ProductType.SUBS
            override val price: Price
                get() = price
            override val title: String
                get() = "product_title"
            override val description: String
                get() = "product_description"
            override val period: Period
                get() = Period(1, Period.Unit.YEAR, "P1Y")
            override val subscriptionOptions: SubscriptionOptions
                get() = subscriptionOptions
            override val defaultOption: SubscriptionOption
                get() = subscriptionOption
            override val purchasingData: PurchasingData
                get() = purchasingData
            override val presentedOfferingIdentifier: String
                get() = "offering_id"

            @Deprecated("Use sku instead", ReplaceWith("id"))
            override val sku: String
                get() = id

            override fun copyWithOfferingId(offeringId: String): StoreProduct {
                error("Not implemented")
            }
        }
        return Offering(
            identifier = "offering_id",
            serverDescription = "server_description",
            metadata = emptyMap(),
            availablePackages = listOf(
                Package("package_id", PackageType.ANNUAL, storeProduct, "offering_id"),
            ),
        )
    }

@Preview(showBackground = true)
@Composable
private fun SettingPreview() {
    val viewModel = object : DebugRevenueCatViewModel {
        override val state: StateFlow<SettingScreenState>
            get() = error("Not implemented")

        override fun toastDisplayed() {
            error("Not implemented")
        }

        override fun purchasePackage(activity: Activity, rcPackage: Package) {
            error("Not implemented")
        }

        override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
            error("Not implemented")
        }

        override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
            error("Not implemented")
        }
    }
    SettingOffering(SettingState.OfferingSetting(offering), activity = Activity(), viewModel)
}
