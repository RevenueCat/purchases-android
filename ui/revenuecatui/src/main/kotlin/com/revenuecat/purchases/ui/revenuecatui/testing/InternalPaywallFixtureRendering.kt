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
 * Validates [offering]'s components paywall without rendering, returning validation error messages (empty
 * = valid). An [Offering] without a components paywall is reported as an error.
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
 * Renders [offering]'s components paywall deterministically and fully offline (no Purchases, network, or
 * Billing) for JVM screenshot tests. Images resolve via [imageResolver] (null fails with a descriptive
 * error); [date] fixes "now" for reproducibility; click handlers are no-ops. Backbone of the
 * `purchases-ui-testing` artifact — not for direct SDK-consumer use.
 *
 * @param renderValidationErrors render validation errors as text instead of throwing.
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
 * Forces all fonts to a system fallback. The consumer's module rarely bundles the dashboard's fonts as
 * `res/font`, and resolving them is non-deterministic and crashes under layoutlib (Paparazzi).
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
