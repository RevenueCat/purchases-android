package com.revenuecat.purchasetester.ui

import android.view.View
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct

@Composable
fun PackageList(
    packages: List<Package>,
    activeSubscriptions: Set<String>,
    onPurchasePackageClicked: (View, Package, Boolean, Boolean) -> Unit,
    onPurchaseProductClicked: (View, StoreProduct, Boolean, Boolean) -> Unit,
    onPurchaseSubscriptionOptionClicked: (View, SubscriptionOption, Boolean, Boolean) -> Unit,
    isPlayStore: Boolean,
) {
    LazyColumn {
        items(packages) { pkg ->
            val isSubscription = pkg.product.type == ProductType.SUBS
            val isActive = activeSubscriptions.contains(pkg.product.id)
            PackageCard(
                currentPackage = pkg,
                isActive = isActive,
                isSubscription = isSubscription,
                isPlayStore = isPlayStore,
                onPurchasePackageClicked = onPurchasePackageClicked,
                onPurchaseProductClicked = onPurchaseProductClicked,
                onPurchaseSubscriptionOptionClicked = onPurchaseSubscriptionOptionClicked,
            )
        }
    }
}

@Preview
@Composable
private fun PackageListPreview() {
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
    PackageList(
        packages = listOf(pkg, pkg),
        activeSubscriptions = emptySet(),
        onPurchasePackageClicked = { _, _, _, _ -> },
        onPurchaseProductClicked = { _, _, _, _ -> },
        onPurchaseSubscriptionOptionClicked = { _, _, _, _ -> },
        isPlayStore = true,
    )
}
