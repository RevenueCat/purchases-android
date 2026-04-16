package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import app.cash.paparazzi.DeviceConfig
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.validatePaywallComponentsDataOrNullForPreviews
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.net.URI
import java.util.Date

private const val MILLIS_2025_04_23 = 1745366400000

/**
 * Renders a paywall screenshot from a filesystem-provided offerings.json.
 *
 * Invoked via:
 * ```
 * ./gradlew :ui:revenuecatui:recordPaparazziBc8Debug \
 *     --tests="*PaywallJsonScreenshotter" \
 *     -Ppaywall.input.dir="/path/to/dir"
 * ```
 *
 * The input directory must contain:
 * - `offerings.json` — SDK-shaped offerings JSON with `ui_config`, `offerings`, and `paywall_components`
 * - Downloaded image assets in the reversed-host directory structure
 *   (e.g. `pawwalls/assets/123.png` for `https://assets.pawwalls.com/123.png`)
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class PaywallJsonScreenshotter internal constructor(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val offering: Offering,
) : BasePaparazziTest(
    testConfig = TestConfig(
        name = "pixel6",
        deviceConfig = DeviceConfig.PIXEL_6,
    ),
) {

    companion object {
        lateinit var inputDir: File
            private set

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): List<Array<Any>> {
            Purchases.logHandler = PrintLnLogHandler

            val inputDirPath = System.getProperty("paywall.input.dir")
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "System property 'paywall.input.dir' is required. " +
                        "Pass -Ppaywall.input.dir=/path/to/dir to Gradle.",
                )
            inputDir = File(inputDirPath)

            val offeringsJsonFile = File(inputDir, "offerings.json")
            require(offeringsJsonFile.exists()) {
                "offerings.json not found in input directory: $inputDirPath"
            }

            val offeringsJson = JSONObject(offeringsJsonFile.readText())

            // Replace each offering's packages with the standard set that PreviewOfferingParser
            // knows how to handle. Custom package identifiers would crash the parser.
            val standardPackages = standardPackagesArray()
            val offeringsArray = offeringsJson.getJSONArray("offerings")
            for (i in 0 until offeringsArray.length()) {
                offeringsArray.getJSONObject(i).put("packages", standardPackages)
            }

            val offeringParser = Class.forName("com.revenuecat.purchases.utils.PreviewOfferingParser")
                .getDeclaredConstructor()
                .apply { isAccessible = true }
                .newInstance()
            val createOfferingsMethod = offeringParser::class.java
                .getMethod("createOfferings", JSONObject::class.java, Map::class.java)

            val offerings = createOfferingsMethod(
                offeringParser,
                offeringsJson,
                emptyMap<String, List<StoreProduct>>(),
            ) as Offerings

            val offering = offerings.current
                ?: error("No current offering found in offerings.json")
            require(offering.paywallComponents != null) {
                "Current offering '${offering.identifier}' has no paywall_components"
            }

            return listOf(arrayOf(offering.identifier, offering))
        }

        @JvmStatic
        @BeforeClass
        fun setup() {
            Dispatchers.setMain(newSingleThreadContext("PaywallJsonScreenshotter-main-dispatcher"))
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            Dispatchers.resetMain()
        }

        private fun standardPackagesArray(): JSONArray = JSONArray().apply {
            val identifiers = listOf(
                "\$rc_lifetime",
                "\$rc_annual",
                "\$rc_six_month",
                "\$rc_three_month",
                "\$rc_two_month",
                "\$rc_monthly",
                "\$rc_weekly",
            )
            for (id in identifiers) {
                put(JSONObject().put("identifier", id))
            }
        }
    }

    @Suppress("TestFunctionName", "FunctionNaming")
    @Test
    fun PaywallJsonScreenshot_Test() {
        screenshotTest {
            when (val result = offering.validatePaywallComponentsDataOrNullForPreviews()!!) {
                is Result.Success -> {
                    val state = offering.toComponentsPaywallState(
                        validationResult = result.value,
                        storefrontCountryCode = "US",
                        dateProvider = { Date(MILLIS_2025_04_23) },
                        purchases = MockPurchasesType(),
                    )

                    ProvidePreviewImageLoader(
                        FilesystemImageLoader(LocalContext.current, inputDir),
                    ) {
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
    }

    private object PrintLnLogHandler : LogHandler {
        override fun v(tag: String, msg: String) {
            println("V [$tag]: $msg")
        }

        override fun d(tag: String, msg: String) {
            println("D [$tag]: $msg")
        }

        override fun i(tag: String, msg: String) {
            println("I [$tag]: $msg")
        }

        override fun w(tag: String, msg: String) {
            println("W [$tag]: $msg")
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            println("E [$tag]: $msg")
            throwable?.printStackTrace()
        }
    }
}

@Suppress("FunctionName")
private fun FilesystemImageLoader(context: Context, inputDir: File): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add { chain ->
                val url = URI(chain.request.data as String)
                val resourcePath = url.host.split('.').dropLast(1).reversed().joinToString("/") + url.path
                val bitmap = BitmapFactory.decodeStream(File(inputDir, resourcePath).inputStream())

                SuccessResult(
                    drawable = bitmap.toDrawable(context.resources),
                    request = chain.request,
                    dataSource = DataSource.DISK,
                )
            }
        }
        .build()
