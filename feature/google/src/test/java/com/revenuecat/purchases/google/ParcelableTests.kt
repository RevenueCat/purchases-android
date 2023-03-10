package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.testParcelization
import org.junit.Test
import org.junit.runner.RunWith
import com.revenuecat.purchases.Package as RevenueCatPackage

@RunWith(AndroidJUnit4::class)
class ParcelableTests {

    private val price = Price(
        "$3.99",
        3990000L,
        "USD"
    )

    private val period = Period(
        0,
        Period.Unit.MONTH,
        "iso date strijng"
    )

    private val phase = PricingPhase(
        period,
        RecurrenceMode.FINITE_RECURRING,
        null,
        price
    )

    @Test
    fun `Price is Parcelable`() {
        testParcelization(price)
    }

    @Test
    fun `Period is Parcelable`() {
        testParcelization(phase)
    }

    @Test
    fun `PricingPhase is Parcelable`() {
        testParcelization(phase)
    }



}
