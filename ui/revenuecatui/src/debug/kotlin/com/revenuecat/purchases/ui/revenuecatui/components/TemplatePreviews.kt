package com.revenuecat.purchases.ui.revenuecatui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Date

private const val DIR_TEMPLATES = "paywall-templates"

/**
 * A PreviewParameterProvider that parses the offerings JSON and provides each offering that has a v2 Paywall.
 */
private class OfferingProvider : PreviewParameterProvider<Offering> {
    private val offeringsJsonFilePath = "$DIR_TEMPLATES/offerings_paywalls_v2_templates.json"
    private val packagesJsonFilePath = "packages.json"

    private val packagesJsonArray = JSONObject(getResourceStream(packagesJsonFilePath).readBytes().decodeToString())
        .getJSONArray("packages")
    private val uiConfigJsonObject = JSONObject(getResourceStream(offeringsJsonFilePath).readUiConfig())

    private val offeringParser = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    private val createOfferingsMethod = offeringParser::class.java
        .getMethod("createOfferings", JSONObject::class.java, Map::class.java)

    // Find all start (inclusive) and end (exclusive) indices of each offering in the JSON.
    private val offeringIndices = getResourceStream(offeringsJsonFilePath).indexOfferings()

    override val values = offeringIndices
        .asSequence()
        .mapNotNull { (start, end) ->
            // Re-open the stream and read only the current offering. While we could keep the stream open for the
            // entirety of the sequence and yield offerings as we encounter them, we found that Emerge Snapshots closes
            // the stream prematurely in this case. To avoid that, we reopen the stream for each offering.
            val offeringJsonString = getResourceStream(offeringsJsonFilePath).readOfferingAt(start, end)
            val offeringJsonObject = JSONObject(offeringJsonString)
            val hasPaywall = offeringJsonObject.optString("paywall_components").isNotBlank()
            if (!hasPaywall) return@mapNotNull null

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
        }

    private fun createOfferings(offeringsJsonObject: JSONObject): Offerings =
        createOfferingsMethod(offeringParser, offeringsJsonObject, emptyMap<String, List<StoreProduct>>()) as Offerings

    /**
     * Finds all start (inclusive) and end (exclusive) indices of each offering in the JSON.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun InputStream.indexOfferings(): List<Pair<Int, Int>> = buildList {
        bufferedReader().use { reader ->
            val assumedNewLineChars = 1 // Assuming either LF or CR, not CRLF.
            // Read until we reach the offerings array.
            var index = 0
            var line: String? = reader.readLine()?.also { index += it.length + assumedNewLineChars }
            while (line != null && !line.contains("\"offerings\": [")) {
                line = reader.readLine()?.also { index += it.length + assumedNewLineChars }
            }

            // Now start reading each offering.
            var c: Int
            var insideObject = false
            var braceCount = 0
            var currentStartIndex: Int? = null
            while (reader.read().also { c = it } != -1) {
                val char = c.toChar()
                // When we encounter the start of an object, start counting braces to be able to handle nested objects.
                if (char == '{') {
                    if (!insideObject) {
                        insideObject = true
                        currentStartIndex = index
                    }
                    braceCount++
                }

                if (char == '}') {
                    braceCount--
                    if (braceCount == 0 && insideObject) {
                        // We have a complete JSON object.
                        val currentEndIndex = index + 1
                        add(currentStartIndex!! to currentEndIndex)
                        currentStartIndex = null
                        insideObject = false
                    }
                }

                // We hit the end of the offerings array.
                if (!insideObject && char == ']') break
                index++
            }
        }
    }

    /**
     * Reopens the file and reads the characters from start (inclusive) to end (exclusive),
     * returning that substring.
     */
    private fun InputStream.readOfferingAt(start: Int, end: Int): String = bufferedReader().use { reader ->
        reader.skip(start.toLong())

        val length = end - start
        val charArray = CharArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val readCount = reader.read(charArray, totalRead, length - totalRead)
            if (readCount == -1) break
            totalRead += readCount
        }

        String(charArray, 0, totalRead)
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

    ProvidePreviewImageLoader(PaywallTemplateImageLoader(LocalContext.current)) {
        LoadedPaywallComponents(
            state = state,
            clickHandler = { },
        )
    }
}

@Suppress("FunctionName")
private fun PaywallTemplateImageLoader(
    context: Context,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add { chain ->
                val url = URI(chain.request.data as String)
                // Create the resourcePath by dropping the TLD, reversing the host, and appending the path.
                val resourcePath = DIR_TEMPLATES + "/" +
                    url.host.split('.').dropLast(1).reversed().joinToString("/") +
                    url.path
                val bitmap = BitmapFactory.decodeStream(getResourceStream(resourcePath))

                SuccessResult(
                    drawable = bitmap.toDrawable(context.resources),
                    request = chain.request,
                    dataSource = DataSource.DISK,
                )
            }
        }
        .build()
}

/**
 *  Reads from the `resources` directory. It uses the ClassLoader on Android, e.g. when the preview is shown on a
 *  device/emulator, and the PROJECT_DIR when the preview is shown in Android Studio.
 *
 *  We could place our files in the res folder and read them in a more straightforward way. However that means we need
 *  a Context to read them, which we don't have access to in a PreviewParameterProvider. It's beneficial to read the
 *  offerings JSON file directly in the PreviewParameterProvider, so we can parse the offering IDs and avoid hardcoding
 *  them.
 *
 *  @param filePath A relative filepath to the file in the `resources` directory.
 */
private fun getResourceStream(filePath: String): InputStream =
    object {}.javaClass.getResource("/$filePath")?.openStream()
        ?: File(BuildConfig.PROJECT_DIR).resolve("src/debug/resources/$filePath").inputStream()

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
