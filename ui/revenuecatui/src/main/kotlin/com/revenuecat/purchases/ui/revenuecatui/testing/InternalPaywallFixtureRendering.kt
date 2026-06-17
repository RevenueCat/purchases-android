package com.revenuecat.purchases.ui.revenuecatui.testing

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.io.InputStream
import java.util.Date

/**
 * Validates the components paywall of this [Offering] without rendering it, returning the list of
 * validation error messages. An empty list means the paywall is valid. This allows test frameworks to
 * provide assertion-style validation without going through composition.
 *
 * Note that an [Offering] without a components paywall (e.g. a legacy paywall-only Offering) is reported
 * as a validation error.
 */
@InternalRevenueCatAPI
public fun validateComponentsPaywallForTesting(offering: Offering, context: Context): List<String> {
    val result = offering.validatePaywallComponentsDataOrNull(context.toFixtureResourceProvider())
        ?: return listOf(noComponentsPaywallMessage(offering))
    return when (result) {
        is Result.Success -> emptyList()
        is Result.Error -> result.value.map { it.message(offering) }
    }
}

/**
 * Renders the components paywall of [offering] in a deterministic, offline manner, for use in JVM
 * screenshot tests (e.g. Paparazzi or Roborazzi). This is the rendering backbone of the
 * `purchases-ui-testing` artifact and is not intended to be called directly by SDK consumers.
 *
 * No [com.revenuecat.purchases.Purchases] instance, network access, or Google Play Billing is required:
 * - [offering] is expected to be built offline, e.g. via
 *   [com.revenuecat.purchases.utils.FixtureOfferingsFactory].
 * - Remote images are resolved through [imageResolver], which maps an image URL to a local [InputStream]
 *   (typically a fixture file recorded by the `recordPaywallFixtures` Gradle task). Returning null fails
 *   the render with a descriptive error.
 * - [date] fixes "current time" so that date-dependent output (e.g. countdowns) is reproducible.
 *
 * Click handlers are no-ops: purchase and navigation flows are out of scope for snapshot tests.
 *
 * @param renderValidationErrors When true, validation errors are rendered as text (mirroring preview
 * behavior) instead of throwing [PaywallComponentsValidationException].
 */
@InternalRevenueCatAPI
@Composable
@Suppress("LongParameterList")
public fun ComponentsPaywallForTesting(
    offering: Offering,
    imageResolver: (url: String) -> InputStream?,
    storefrontCountryCode: String?,
    date: Date,
    renderValidationErrors: Boolean,
    modifier: Modifier = Modifier,
) {
    val resourceProvider = LocalContext.current.toFixtureResourceProvider()
    val validationResult = remember(offering) {
        offering.validatePaywallComponentsDataOrNull(resourceProvider)
    }

    when (validationResult) {
        is Result.Success -> {
            val state = remember(offering, storefrontCountryCode, date) {
                offering.toComponentsPaywallState(
                    validationResult = validationResult.value,
                    storefrontCountryCode = storefrontCountryCode,
                    dateProvider = { date },
                    purchases = MockPurchasesType(),
                )
            }
            ProvidePreviewImageLoader(fixtureImageLoader(imageResolver)) {
                LoadedPaywallComponents(
                    state = state,
                    clickHandler = { },
                    modifier = modifier,
                )
            }
        }

        is Result.Error -> ValidationErrors(
            errors = validationResult.value.map { it.message(offering) },
            renderValidationErrors = renderValidationErrors,
        )

        null -> ValidationErrors(
            errors = listOf(noComponentsPaywallMessage(offering)),
            renderValidationErrors = renderValidationErrors,
        )
    }
}

/**
 * Renders [errors] as text when [renderValidationErrors] is true, throws otherwise.
 */
@Composable
private fun ValidationErrors(errors: List<String>, renderValidationErrors: Boolean) {
    if (!renderValidationErrors) throw PaywallComponentsValidationException(errors)
    Column {
        Text("Encountered validation errors:")
        errors.forEach { error -> Text(error) }
    }
}

private fun PaywallValidationError.message(offering: Offering): String = associatedErrorString(offering)

private fun Context.toFixtureResourceProvider(): ResourceProvider =
    FontFallbackResourceProvider(toResourceProvider())

/**
 * Wraps a [ResourceProvider] so that all fonts resolve to a system/generic fallback. Snapshot tests run
 * in the consumer's module, which generally does not bundle the dashboard's custom fonts as `res/font`
 * resources; attempting to resolve them is non-deterministic and crashes under layoutlib (Paparazzi).
 * Forcing the fallback keeps offline rendering deterministic and independent of the consumer's resources.
 */
internal class FontFallbackResourceProvider(
    private val delegate: ResourceProvider,
) : ResourceProvider by delegate {
    override fun getResourceIdentifier(name: String, type: String): Int =
        if (type == "font") 0 else delegate.getResourceIdentifier(name, type)

    override fun getXmlFontFamily(resourceId: Int): FontFamily? = null

    override fun getCachedFontFamilyOrStartDownload(
        fontInfo: UiConfig.AppConfig.FontsConfig.FontInfo.Name,
    ): DownloadedFontFamily? = null
}

private fun noComponentsPaywallMessage(offering: Offering): String =
    "Offering '${offering.identifier}' has no components paywall. Only Paywalls V2 (components) are " +
        "supported. If this Offering uses a legacy (V1) paywall, it cannot be snapshot-tested with this kit. " +
        "If it should have a components paywall, the fixture may be missing 'paywall_components' or " +
        "'ui_config', or it may have been recorded with paywall features this SDK version cannot parse — " +
        "try re-recording the fixture or upgrading the SDK."

/**
 * An [ImageLoader] that resolves every image URL through [imageResolver] instead of the network.
 */
@Composable
private fun fixtureImageLoader(imageResolver: (url: String) -> InputStream?): ImageLoader {
    val context = LocalContext.current
    return remember(context, imageResolver) {
        ImageLoader.Builder(context)
            .components {
                add { chain ->
                    val url = chain.request.data.toString()
                    val stream = imageResolver(url)
                        ?: error(
                            "No fixture image found for URL: $url. Make sure your recorded fixtures " +
                                "include this image, or re-run the recordPaywallFixtures Gradle task.",
                        )
                    val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                        ?: error("Could not decode fixture image for URL: $url.")
                    SuccessResult(
                        drawable = bitmap.toDrawable(context.resources),
                        request = chain.request,
                        dataSource = DataSource.DISK,
                    )
                }
            }
            .build()
    }
}
