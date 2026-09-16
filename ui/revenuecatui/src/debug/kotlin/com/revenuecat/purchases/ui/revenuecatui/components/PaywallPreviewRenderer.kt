@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emergetools.snapshots.annotations.EmergeSnapshotConfig
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.util.Date

/**
 * Loads `PaywallComponentsData` JSON and builds a preview [Offering].
 *
 * This lives in the debug source set so it is compiled out of release SDK artifacts.
 *
 * Usage in a future PR:
 * ```
 * @Preview
 * @Composable
 * private fun MyFeaturePreview() {
 *     PaywallPreviewFromJSON(json = MY_JSON)
 * }
 * ```
 */
internal object PaywallPreviewRenderer {

    data class Fixture(
        val offering: Offering,
        val paywallComponents: Offering.PaywallComponents,
    )

    /**
     * Dashboard-shaped `PaywallComponentsData` JSON for the sample preview and unit tests.
     */
    const val SAMPLE_JSON = """
    {
      "template_name": "components",
      "asset_base_url": "https://assets.pawwalls.com",
      "components_config": {
        "base": {
          "background": {
            "type": "color",
            "value": {
              "light": { "type": "hex", "value": "#ffffffff" }
            }
          },
          "stack": {
            "type": "stack",
            "components": [
              {
                "type": "text",
                "text_lid": "title",
                "color": {
                  "light": { "type": "hex", "value": "#111111ff" }
                },
                "font_size": 24,
                "font_weight": "bold",
                "horizontal_alignment": "center",
                "size": {
                  "width": { "type": "fit" },
                  "height": { "type": "fit" }
                }
              }
            ],
            "size": {
              "width": { "type": "fill" },
              "height": { "type": "fit" }
            },
            "dimension": {
              "type": "vertical",
              "alignment": "center",
              "distribution": "center"
            }
          }
        }
      },
      "components_localizations": {
        "en_US": { "title": "JSON paywall preview" }
      },
      "default_locale": "en_US"
    }
    """

    val defaultPackages: List<Package> = listOf(
        TestData.Packages.weekly,
        TestData.Packages.monthly,
        TestData.Packages.annual,
    )

    fun load(
        json: String,
        offeringIdentifier: String = "json-preview",
        serverDescription: String = "JSON paywall preview",
        packages: List<Package> = defaultPackages,
        uiConfig: UiConfig = previewUiConfig(),
    ): Fixture {
        val data = jsonDecoder.decodeFromString(PaywallComponentsData.serializer(), json)
        val paywallComponents = Offering.PaywallComponents(uiConfig, data)
        val offering = Offering(
            identifier = offeringIdentifier,
            serverDescription = serverDescription,
            metadata = emptyMap(),
            availablePackages = packages,
            paywallComponents = paywallComponents,
        )
        return Fixture(offering = offering, paywallComponents = paywallComponents)
    }
}

/**
 * Compose preview host that decodes local paywall JSON and renders [LoadedPaywallComponents].
 */
@Composable
internal fun PaywallPreviewFromJSON(
    json: String,
    modifier: Modifier = Modifier,
    offeringIdentifier: String = "json-preview",
    serverDescription: String = "JSON paywall preview",
    packages: List<Package> = PaywallPreviewRenderer.defaultPackages,
) {
    val loadResult = remember(json, offeringIdentifier, serverDescription, packages) {
        runCatching {
            PaywallPreviewRenderer.load(
                json = json,
                offeringIdentifier = offeringIdentifier,
                serverDescription = serverDescription,
                packages = packages,
            )
        }
    }
    val fixture = loadResult.getOrNull()
    if (fixture == null) {
        Text("Unable to load JSON paywall preview:\n${loadResult.exceptionOrNull()?.message}")
        return
    }

    when (val result = fixture.offering.validatePaywallComponentsDataOrNullForPreviews()) {
        is Result.Success -> {
            val state = fixture.offering.toComponentsPaywallState(
                validationResult = result.value,
                storefrontCountryCode = "US",
                dateProvider = { Date(MILLIS_2025_01_25) },
                purchases = MockPurchasesType(),
            )
            ProvidePreviewImageLoader(previewImageLoader()) {
                LoadedPaywallComponents(
                    state = state,
                    clickHandler = { },
                    modifier = modifier.fillMaxSize(),
                )
            }
        }
        is Result.Error -> {
            Column {
                Text("Encountered validation errors:")
                result.value.forEach { error -> Text(error.toString()) }
            }
        }
        null -> Text("Offering has no paywall components")
    }
}

@EmergeSnapshotConfig(ignore = true)
@Preview(showSystemUi = true)
@Composable
private fun PaywallPreviewFromJSON_Sample() {
    PaywallPreviewFromJSON(json = PaywallPreviewRenderer.SAMPLE_JSON)
}

@OptIn(ExperimentalSerializationApi::class)
private val jsonDecoder = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
