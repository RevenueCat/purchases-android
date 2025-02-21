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
import java.io.InputStream
import java.util.Date

/**
 * A PreviewParameterProvider that parses the offerings JSON and provides each offering that has a v2 Paywall.
 */
private class OfferingProvider : PreviewParameterProvider<Offering> {
    private val offeringsJsonFileName = "offerings_paywalls_v2_templates.json"
    private val packagesJsonFileName = "packages.json"

    private val packagesJsonArray = JSONObject(getResourceStream(packagesJsonFileName).readBytes().decodeToString())
        .getJSONArray("packages")
    private val uiConfigJsonObject = JSONObject(getResourceStream(offeringsJsonFileName).readUiConfig())

    private val offeringParser = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    private val createOfferingsMethod = offeringParser::class.java
        .getMethod("createOfferings", JSONObject::class.java, Map::class.java)

    override val values: Sequence<Offering> = sequence {
        var index = 0
        @Suppress("LoopWithTooManyJumpStatements")
        while (true) {
            // Reopen the stream for each offering and skip previous ones. While we could keep the stream open for the
            // entirety of the sequence and yield offerings as we encounter them, we found that Emerge Snapshots closes
            // the stream prematurely in this case. To avoid that, we reopen the stream for each offering.
            val offeringJsonObject = getResourceStream(offeringsJsonFileName)
                .offeringJsonStringSequence()
                .get(index)
                ?.let { JSONObject(it) }
                ?: break // Break if there are no more offerings.

            val hasPaywall = offeringJsonObject.optString("paywall_components").isNotBlank()
            // Skip any offering that doesn't have a paywall.
            if (!hasPaywall) {
                index++
                continue
            }

            // Ensure that the offering has all packages.
            offeringJsonObject.put("packages", packagesJsonArray)
            val offeringId = offeringJsonObject.getString("identifier")
            val offeringsJsonObject = JSONObject()
                .put("current_offering_id", offeringId)
                .put("offerings", JSONArray().put(offeringJsonObject))
                .put("ui_config", uiConfigJsonObject)

            createOfferings(offeringsJsonObject)
                .current
                ?.takeUnless { it.paywallComponents == null }
                ?.also { offering -> yield(offering) }

            index++
        }
    }

    private fun createOfferings(offeringsJsonObject: JSONObject): Offerings =
        createOfferingsMethod(offeringParser, offeringsJsonObject, emptyMap<String, List<StoreProduct>>()) as Offerings

    private fun <T> Sequence<T>.get(index: Int): T? =
        drop(index).firstOrNull()
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
private fun getResourceStream(fileName: String): InputStream =
    object {}.javaClass.getResource("/$fileName")?.openStream()
        ?: File(BuildConfig.PROJECT_DIR).resolve("src/debug/resources/$fileName").inputStream()

/**
 * A very limited streaming JSON parser that will look for offerings and will return each one in a Sequence.
 */
@Suppress("CyclomaticComplexMethod")
private fun InputStream.offeringJsonStringSequence(): Sequence<String> = sequence {
    bufferedReader().use { reader ->
        // Read until we reach the offerings array.
        var line: String? = reader.readLine()
        while (line != null && !line.contains("\"offerings\": [")) line = reader.readLine()

        // Now start reading each offering.
        var c: Int
        val stringBuilder = StringBuilder()
        var insideObject = false
        var braceCount = 0
        while (reader.read().also { c = it } != -1) {
            val char = c.toChar()
            // When we encounter the start of an object, start counting braces to be able to handle nested objects.
            if (char == '{') {
                if (!insideObject) {
                    insideObject = true
                    stringBuilder.clear()
                }
                braceCount++
            }

            if (insideObject) stringBuilder.append(char)

            if (char == '}') {
                braceCount--
                if (braceCount == 0 && insideObject) {
                    // We have a complete JSON object.
                    yield(stringBuilder.toString())
                    insideObject = false
                }
            }

            // We hit the end of the offerings array.
            if (!insideObject && char == ']') break
        }
    }
}

/**
 * Reads the ui_config from this stream without reading the entire file into memory.
 */
private fun InputStream.readUiConfig(): String =
    bufferedReader().use { reader ->
        var line: String? = reader.readLine()

        // Read until we encounter the start of the ui_config object.
        while (line != null && !line.contains("\"ui_config\": {")) line = reader.readLine()

        buildString {
            appendLine("{")
            // Count braces to be able to handle nested objects.
            var braceCount = 1
            while (reader.readLine().also { line = it } != null) {
                appendLine(line)
                braceCount += line!!.count { it == '{' } - line!!.count { it == '}' }
                if (braceCount <= 0) break
            }
        }
    }
