package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date

private object PreviewOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        // Ignoring productsById and just returning a product based on the identifier in the packageJson.
        val identifier = packageJson.getString("identifier")
        val packageType = PackageType.values().first { packageType -> packageType.identifier == identifier }

        return when (packageType) {
            PackageType.LIFETIME -> TestData.Packages.lifetime.product
            PackageType.ANNUAL -> TestData.Packages.annual.product
            PackageType.SIX_MONTH -> TestData.Packages.semester.product
            PackageType.THREE_MONTH -> TestData.Packages.quarterly.product
            PackageType.TWO_MONTH -> TestData.Packages.bimonthly.product
            PackageType.MONTHLY -> TestData.Packages.monthly.product
            PackageType.WEEKLY -> TestData.Packages.weekly.product
            else -> null
        }
    }
}

private class OfferingProvider : PreviewParameterProvider<Offering> {

    // We could place the JSON file in the res folder and read it in a more straightforward way. However that means we
    // need a Context to read it, which we don't have access to in a PreviewParameterProvider. It's beneficial to read
    // the file directly in the PreviewParameterProvider, so we can parse the offering IDs and avoid hardcoding them.
    private val jsonFileName = "offerings_paywalls_v2_templates.json"

    // The ClassLoader is used on Android, e.g. when the preview is shown on a device/emulator, and the PROJECT_DIR is
    // used when the preview is shown in Android Studio.
    private val jsonString = object {}.javaClass.getResource("/$jsonFileName")?.readText()
        ?: File(BuildConfig.PROJECT_DIR).resolve("src/debug/resources/$jsonFileName").readText()
    private val json = JSONObject(jsonString)

    private val offeringIds: List<String> = json.getJSONArray("offerings")
        .mapNotNull { index ->
            val offering = getJSONObject(index)
            val hasPaywall = offering.optString("paywall_components").isNotBlank()
            if (hasPaywall) offering.getString("identifier") else null
        }
    private val offerings = PreviewOfferingParser.createOfferings(json, emptyMap())

    override val values: Sequence<Offering> = offeringIds.asSequence()
        .mapNotNull { offeringId ->
            offerings.getOffering(offeringId)
                ?.takeUnless { offering -> offering.paywallComponents == null }
        }

    private inline fun <R : Any> JSONArray.mapNotNull(transform: JSONArray.(index: Int) -> R?): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until length()) {
            val transformed = transform(i)
            if (transformed != null) result.add(transformed)
        }
        return result
    }
}

@Preview
@Composable
private fun PaywallComponentsTemplate_Preview(
    @PreviewParameter(OfferingProvider::class) offering: Offering,
) {
    val validationResult = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    val state = offering.toComponentsPaywallState(
        validationResult = validationResult,
        activelySubscribedProductIds = emptySet(),
        purchasedNonSubscriptionProductIds = emptySet(),
        storefrontCountryCode = "US",
        dateProvider = { Date() },
    )

    LoadedPaywallComponents(
        state = state,
        clickHandler = { },
    )
}
