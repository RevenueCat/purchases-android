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

    private val packagesJsonArray = JSONObject(getResource(packagesJsonFileName).decodeToString())
        .getJSONArray("packages")
    private val uiConfigJsonObject = JSONObject(getResource(offeringsJsonFileName).inputStream().readUiConfig())

    private val offeringParser = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    private val createOfferingsMethod = offeringParser::class.java
        .getMethod("createOfferings", JSONObject::class.java, Map::class.java)

    override val values: Sequence<Offering> = getResourceStream(offeringsJsonFileName)
        .offeringJsonStringSequence()
        .mapNotNull { offeringJsonString ->
            val offeringJson = JSONObject(offeringJsonString)
            val hasPaywall = offeringJson.optString("paywall_components").isNotBlank()

            if (!hasPaywall) return@mapNotNull null

            val offeringId = offeringJson.getString("identifier")

            val offeringJsonObject = offeringJson.put("packages", packagesJsonArray)
            val offeringsJsonObject = JSONObject()
                .put("current_offering_id", offeringId)
                .put("offerings", JSONArray().put(offeringJsonObject))
                .put("ui_config", uiConfigJsonObject)
            createOfferings(offeringsJsonObject)
                .current
                ?.takeUnless { offering -> offering.paywallComponents == null }
        }
    // Re-enable: .sortedBy { it.identifier }

    private fun createOfferings(offeringsJsonObject: JSONObject): Offerings =
        createOfferingsMethod(offeringParser, offeringsJsonObject, emptyMap<String, List<StoreProduct>>()) as Offerings
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

private fun getResource(fileName: String): ByteArray =
    getResourceStream(fileName).readBytes()

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
 * Returns a sequence of JSON strings, each representing a single offering.
 * This simplistic parser assumes that the JSON structure is well-formed and that
 * object boundaries (i.e. `{` and `}`) do not appear in string literals.
 */
private fun InputStream.offeringJsonStringSequence(): Sequence<String> = sequence {
    val reader = bufferedReader()
    // Read until we reach the offerings array
    var line: String? = reader.readLine()
    while (line != null && !line.contains("\"offerings\": [")) {
        line = reader.readLine()
    }

    // Now start reading each offering.
    var c: Int
    val sb = StringBuilder()
    var insideObject = false
    var braceCount = 0
    while (reader.read().also { c = it } != -1) {
        val char = c.toChar()
        // When we encounter the start of an object, initialize our counter.
        if (char == '{') {
            if (!insideObject) {
                insideObject = true
                sb.clear()
            }
            braceCount++
        }
        if (insideObject) {
            sb.append(char)
        }
        if (char == '}') {
            braceCount--
            if (braceCount == 0 && insideObject) {
                // We have a complete JSON object.
                yield(sb.toString())
                insideObject = false
            }
        }
        // Optionally, stop if we hit the end of the offerings array.
        if (!insideObject && char == ']') {
            println("TESTING hit the end!")
            break
        }
    }
    println("TESTING c: $c")
    reader.close()
}

private fun InputStream.readUiConfig(): String {
    val reader = bufferedReader()
    var line: String?
    var uiConfigStarted = false
    val sb = StringBuilder()
    var braceCount = 0

    while (reader.readLine().also { line = it } != null) {
        if (!uiConfigStarted) {
            // Look for the "ui_config" key.
            if (line!!.contains("\"ui_config\"")) {
                // Look for the first '{' on the same line.
                val braceIndex = line!!.indexOf("{")
                if (braceIndex != -1) {
                    uiConfigStarted = true
                    val subLine = line!!.substring(braceIndex)
                    sb.append(subLine)
                    // Count braces: add for '{', subtract for '}'.
                    braceCount += subLine.count { it == '{' } - subLine.count { it == '}' }
                    if (braceCount <= 0) break // The object closed on the same line.
                }
            }
        } else {
            // Already in ui_config: append the full line.
            sb.append("\n").append(line)
            braceCount += line!!.count { it == '{' } - line!!.count { it == '}' }
            if (braceCount <= 0) {
                // Finished reading the ui_config object.
                break
            }
        }
    }
    reader.close()
    check(uiConfigStarted && braceCount == 0) { "Failed to locate or parse ui_config in JSON" }
    return sb.toString()
}
