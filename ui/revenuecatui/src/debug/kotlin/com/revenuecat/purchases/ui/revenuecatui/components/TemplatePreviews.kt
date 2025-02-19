package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date

private class OfferingProvider : PreviewParameterProvider<Offering> {
    private val offeringsJsonFileName = "offerings_paywalls_v2_templates.json"
    private val packagesJsonFileName = "packages.json"

    private val packagesJsonArray =
        JSONObject(getResource(packagesJsonFileName).decodeToString()).getJSONArray("packages")
    private val offeringsJsonObject = JSONObject(getResource(offeringsJsonFileName).decodeToString()).apply {
        // Make sure every offering has all packages.
        getJSONArray("offerings").forEach { index -> getJSONObject(index).put("packages", packagesJsonArray) }
    }

    private val offeringIds: List<String> = offeringsJsonObject.getJSONArray("offerings")
        .mapNotNull { index ->
            val offering = getJSONObject(index)
            val hasPaywall = offering.optString("paywall_components").isNotBlank()
            if (hasPaywall) offering.getString("identifier") else null
        }

    // Using reflection to create our Offerings, so the OfferingParser can stay internal to the purchases SDK.
    private val offerings = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
        .let { cls ->
            val offeringParser = cls.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            val createOfferings = cls.getMethod("createOfferings", JSONObject::class.java, Map::class.java)
            createOfferings(offeringParser, offeringsJsonObject, emptyMap<String, List<StoreProduct>>()) as Offerings
        }

    override val values: Sequence<Offering> = offeringIds.asSequence()
        .sorted()
        .mapNotNull { offeringId ->
            offerings.getOffering(offeringId)
                ?.takeUnless { offering -> offering.paywallComponents == null }
        }

    private inline fun <T : Any> JSONArray.mapNotNull(transform: JSONArray.(index: Int) -> T?): List<T> {
        val result = mutableListOf<T>()
        for (i in 0 until length()) {
            val transformed = transform(i)
            if (transformed != null) result.add(transformed)
        }
        return result
    }

    private inline fun JSONArray.forEach(block: JSONArray.(index: Int) -> Unit) {
        for (i in 0 until length()) {
            block(i)
        }
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

/**
 *  Reads from the `resources` directory. It uses the ClassLoader on Android, e.g. when the preview is shown on a
 *  device/emulator, and the PROJECT_DIR when the preview is shown in Android Studio.
 *
 *  We could place our files in the res folder and read them in a more straightforward way. However that means we need
 *  a Context to read them, which we don't have access to in a PreviewParameterProvider. It's beneficial to read the
 *  offerings JSON file directly in the PreviewParameterProvider, so we can parse the offering IDs and avoid hardcoding
 *  them.
 */
private fun getResource(fileName: String): ByteArray =
    object {}.javaClass.getResource("/$fileName")?.readBytes()
        ?: File(BuildConfig.PROJECT_DIR).resolve("src/debug/resources/$fileName").readBytes()
