package com.revenuecat.purchases.ui.debugview.models

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions

@Suppress("MagicNumber")
internal val testOffering: Offering
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
            override val name: String
                get() = "product_title"
            override val title: String
                get() = "product_title (App name)"
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
            paywall = null,
        )
    }
