package com.revenuecat.purchasetester.ui

import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.amazonProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchasetester.R
import com.revenuecat.purchasetester.SubscriptionOptionExtensions.toButtonString

@Composable
fun InfoRow(header: String, detail: String?) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = header, fontWeight = FontWeight.Bold)
        Text(text = detail.takeUnless { it.isNullOrEmpty() } ?: "None")
    }
}

@Suppress("LongMethod")
@Composable
fun PackageCard(
    currentPackage: Package,
    isActive: Boolean,
    isSubscription: Boolean,
    isPlayStore: Boolean,
    onPurchasePackageClicked: (View, Package, Boolean, Boolean) -> Unit,
    onPurchaseProductClicked: (View, StoreProduct, Boolean, Boolean) -> Unit,
    onPurchaseSubscriptionOptionClicked: (View, SubscriptionOption, Boolean, Boolean) -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var detailsVisible = remember { mutableStateOf(false) }
    var selectedSubscriptionOption: MutableState<SubscriptionOption?> = remember {
        mutableStateOf(currentPackage.product.defaultOption)
    }
    var isUpgrade = remember { mutableStateOf(false) }
    var isPersonalized = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { detailsVisible.value = !detailsVisible.value }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = currentPackage.product.title + if (isActive) " (active)" else "",
                fontWeight = FontWeight.Normal
            )
            Text(text = currentPackage.product.description, modifier = Modifier.padding(vertical = 2.dp))
            InfoRow(header = "Sku:", detail = currentPackage.product.id)
            InfoRow(
                header = "Package Type:",
                detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                    "custom -> ${currentPackage.packageType.identifier}"
                } else {
                    currentPackage.packageType.toString()
                }
            )
            if (!isSubscription) {
                InfoRow(header = "One Time Price:", detail = currentPackage.product.price.formatted)
            }
            if (isSubscription && isPlayStore) {
                Text(
                    text = stringResource(id = R.string.subscription_options),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val options = currentPackage.product.subscriptionOptions
                val defaultOption = currentPackage.product.defaultOption
                options?.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedSubscriptionOption.value?.id == option.id ||
                                (options.size == 1 && selectedSubscriptionOption.value == null),
                            onClick = { selectedSubscriptionOption.value = option }
                        )
                        Text(text = option.toButtonString(option == defaultOption))
                    }
                }
            }
            if (isPlayStore) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isUpgrade.value, onCheckedChange = { isUpgrade.value = it })
                    Text(text = "Is product change?")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPersonalized.value, onCheckedChange = { isPersonalized.value = it })
                    Text(text = "Is personalized price?")
                }
            }
            Button(
                onClick = {
                    onPurchasePackageClicked(
                        view,
                        currentPackage,
                        isUpgrade.value,
                        isPersonalized.value
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Buy package (default option)", fontSize = 10.sp)
            }
            Button(
                onClick = {
                    onPurchaseProductClicked(
                        view,
                        currentPackage.product,
                        isUpgrade.value,
                        isPersonalized.value
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(text = "Buy product (default option)", fontSize = 10.sp)
            }
            if (isPlayStore) {
                Button(
                    onClick = {
                        val selected = selectedSubscriptionOption.value
                        val error = if (isSubscription && selected == null) {
                            "Please choose subscription option first"
                        } else {
                            null
                        }
                        if (error != null) {
                            MaterialAlertDialogBuilder(context)
                                .setMessage(error)
                                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                .show()
                        } else if (selected != null) {
                            onPurchaseSubscriptionOptionClicked(
                                view,
                                selected,
                                isUpgrade.value,
                                isPersonalized.value
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.End)
                ) {
                    Text(text = "Buy option")
                }
            }
            if (detailsVisible.value) {
                InfoRow(
                    header = "Product JSON",
                    detail = currentPackage.product.googleProduct?.productDetails?.toString()
                        ?: currentPackage.product.amazonProduct?.originalProductJSON.toString()
                )
            }
        }
    }
}

@Preview
@Composable
private fun PackageCardPreview() {
    val product = TestStoreProduct(
        id = "monthly",
        name = "Monthly",
        title = "Monthly (Sample)",
        description = "Sample description",
        price = Price(formatted = "$4.99", currencyCode = "USD", amountMicros = 4_990_000),
        period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
    )
    val pkg = Package(
        identifier = "monthly",
        packageType = PackageType.MONTHLY,
        product = product,
        offering = "offering",
    )
    PackageCard(
        currentPackage = pkg,
        isActive = true,
        isSubscription = true,
        isPlayStore = true,
        onPurchasePackageClicked = { _, _, _, _ -> },
        onPurchaseProductClicked = { _, _, _, _ -> },
        onPurchaseSubscriptionOptionClicked = { _, _, _, _ -> },
    )
}
