@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.toRecurrenceMode
import com.revenuecat.purchases.ui.revenuecatui.composables.AppIcon
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData

@JvmSynthetic
@Composable
internal fun PromotionalOfferView(
    promotionalOfferData: PromotionalOfferData,
    onAccept: (SubscriptionOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        AppIcon(
            modifier = Modifier
                .padding(top = 48.dp, bottom = 16.dp)
                .size(100.dp),
        )

        Text(
            text = promotionalOfferData.configuredPromotionalOffer.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = promotionalOfferData.configuredPromotionalOffer.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onAccept(promotionalOfferData.subscriptionOption)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text(promotionalOfferData.localizedPricingPhasesDescription)
        }
        OutlinedButton(
            onClick = { onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text("No Thanks")
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun PromotionalOfferViewPreview() {
    val promoOffer = CustomerCenterConfigTestData.customerCenterData()
        .getManagementScreen()?.paths?.first {
            it.type == CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST
        }!!.promotionalOffer!!
    val data = PromotionalOfferData(
        promoOffer,
        subscriptionOption = stubSubscriptionOption(
            "rc-cancel-offer",
            "monthly",
        ),
        originalPath = CustomerCenterConfigData.HelpPath(
            id = "1",
            title = "Didn't receive purchase",
            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
        ),
        localizedPricingPhasesDescription = "1 month for $7.99, then $9.99/mth",
    )
    PromotionalOfferView(
        data,
        onAccept = {},
        onDismiss = {},
    )
}

@SuppressWarnings("LongParameterList")
private fun stubSubscriptionOption(
    id: String,
    productId: String,
    duration: Period = Period(1, Period.Unit.MONTH, "P1M"),
    pricingPhases: List<PricingPhase> = listOf(stubPricingPhase(billingPeriod = duration)),
    presentedOfferingContext: PresentedOfferingContext? = null,
    installmentsInfo: InstallmentsInfo? = null,
): SubscriptionOption = object : SubscriptionOption {
    override val id: String
        get() = id
    override val pricingPhases: List<PricingPhase>
        get() = pricingPhases
    override val tags: List<String>
        get() = listOf("tag")
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier
    override val presentedOfferingContext: PresentedOfferingContext?
        get() = presentedOfferingContext
    override val purchasingData: PurchasingData
        get() = StubPurchasingData(
            productId = productId,
        )
    override val installmentsInfo: InstallmentsInfo?
        get() = installmentsInfo
}

@SuppressWarnings("MagicNumber")
private fun stubPricingPhase(
    billingPeriod: Period = Period(1, Period.Unit.MONTH, "P1M"),
    priceCurrencyCodeValue: String = "USD",
    price: Double = 4.99,
    recurrenceMode: Int = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
    billingCycleCount: Int = 0,
): PricingPhase = PricingPhase(
    billingPeriod,
    recurrenceMode.toRecurrenceMode(),
    billingCycleCount,
    Price(
        if (price == 0.0) "Free" else "${'$'}$price",
        price.times(1_000_000).toLong(),
        priceCurrencyCodeValue,
    ),
)

private data class StubPurchasingData(
    override val productId: String,
) : PurchasingData {
    override val productType: ProductType
        get() = ProductType.SUBS
}
