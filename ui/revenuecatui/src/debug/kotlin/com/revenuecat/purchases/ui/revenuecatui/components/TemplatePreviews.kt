@file:Suppress("MatchingDeclarationName")

package com.revenuecat.purchases.ui.revenuecatui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
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
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Date

private const val MILLIS_2025_04_23 = 1745366400000
private const val INDEX_FILE_NAME = "offerings_folders.index"
private const val OFFERINGS_JSON_FILE_NAME = "offerings.json"

internal data class PaywallResources(
    val offering: Offering,
    val parentFolder: String,
)

/**
 * A PreviewParameterProvider that parses the offerings JSON and provides each offering that has a v2 Paywall.
 */
internal class PaywallResourcesProvider : PreviewParameterProvider<PaywallResources> {
    private val offeringParser = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    private val createOfferingsMethod = offeringParser::class.java
        .getMethod("createOfferings", JSONObject::class.java, Map::class.java)

    private val offeringJsonFiles = getAllOfferingsJsonFiles()

    override val values = offeringJsonFiles
        .asSequence()
        .flatMap { (folder, file) ->
            val offeringsJsonFilePath = "$folder/$file"
            val uiConfig = JSONObject(getResourceStream(offeringsJsonFilePath).readUiConfig())
            val packagesArray = JSONObject(getResourceStream("packages.json").readBytes().decodeToString())
                .getJSONArray("packages")
            val indices = getResourceStream(offeringsJsonFilePath).indexOfferings()

            indices.mapNotNull { (start, end) ->
                val offeringJsonString = getResourceStream(offeringsJsonFilePath).readOfferingAt(start, end)
                val offeringJsonObject = JSONObject(offeringJsonString)
                val hasPaywall = offeringJsonObject.optString("paywall_components").isNotBlank()
                if (!hasPaywall) return@mapNotNull null

                offeringJsonObject.put("packages", packagesArray)
                val offeringId = offeringJsonObject.getString("identifier")
                val offeringsJsonObject = JSONObject()
                    .put("current_offering_id", offeringId)
                    .put("offerings", JSONArray().put(offeringJsonObject))
                    .put("ui_config", uiConfig)

                createOfferings(offeringsJsonObject)
                    .current
                    ?.takeUnless { it.paywallComponents == null }
                    ?.let { offering ->
                        PaywallResources(
                            offering = offering,
                            parentFolder = folder,
                        )
                    }
            }
        }.sortedBy { it.offering.identifier }

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

/**
 * To render this preview, make sure the paywall-preview-resources submodule is properly initialized.
 * 1. `git submodule init upstream/paywall-preview-resources`
 * 2. `git submodule update upstream/paywall-preview-resources`
 *
 * You'll need to run step 2 every time paywall-preview-resources is updated.
 */
@Preview
@Composable
internal fun PaywallComponentsTemplate_Preview(
    @PreviewParameter(PaywallResourcesProvider::class) paywall: PaywallResources,
) {
    val offering = paywall.offering
    val parentFolder = paywall.parentFolder
    // validatePaywallComponentsDataOrNullForPreviews should only return null if the Offering has no paywallComponents,
    // but we filter those out in the PaywallResourcesProvider.
    when (val result = offering.validatePaywallComponentsDataOrNullForPreviews()!!) {
        is Result.Success -> {
            val validationResult = result.value
            val state = offering.toComponentsPaywallState(
                validationResult = validationResult,
                storefrontCountryCode = "US",
                dateProvider = { Date(MILLIS_2025_04_23) },
                purchases = MockPurchasesType(),
            )

            ProvidePreviewImageLoader(PaywallTemplateImageLoader(LocalContext.current, parentFolder)) {
                LoadedPaywallComponents(
                    state = state,
                    clickHandler = { },
                )
            }
        }
        is Result.Error -> {
            Column {
                Text("Encountered validation errors:")
                result.value.forEach { error -> Text(error.toString()) }
            }
        }
    }
}

@Suppress("FunctionName")
private fun PaywallTemplateImageLoader(
    context: Context,
    parentFolder: String,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add { chain ->
                val url = URI(chain.request.data as String)
                // Create the resourcePath by dropping the TLD, reversing the host, and appending the path.
                val resourcePath = parentFolder + "/" +
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
 * Reads from the index file to determine all parent folders containing offerings.json files.
 */
private fun getAllOfferingsJsonFiles(): List<Pair<String, String>> =
    getResourceStream(INDEX_FILE_NAME)
        .readBytes()
        .decodeToString()
        .lineSequence()
        .filterNot { it.startsWith("#") }
        .filterNot { it.isBlank() }
        .map { parentFolder -> parentFolder to OFFERINGS_JSON_FILE_NAME }
        .toList()

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
        ?: File(BuildConfig.PROJECT_DIR)
            .resolve("../../upstream/paywall-preview-resources/resources/$filePath")
            .inputStream()

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
