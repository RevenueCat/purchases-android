package com.revenuecat.purchases.paywallfixtures.internal

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixtureRecorderTest {

    private lateinit var server: MockWebServer
    private lateinit var outputDir: File

    private val imageBytes = byteArrayOf(1, 2, 3, 4)

    @BeforeTest
    fun setUp() {
        outputDir = createTempDirectory().toFile()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path!!.endsWith("/offerings") -> MockResponse()
                    .setBody(offeringsResponse())
                    .addHeader("Content-Type", "application/json")

                request.path!!.endsWith(".webp") -> MockResponse()
                    .setBody(okio.Buffer().write(imageBytes))

                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
        outputDir.deleteRecursively()
    }

    private fun baseUrl() = server.url("/").toString().removeSuffix("/")

    private fun offeringsResponse(): String = JSONObject(
        """
        {
          "current_offering_id": "default",
          "offerings": [
            {
              "identifier": "default",
              "description": "With components paywall",
              "packages": [{"identifier": "${'$'}rc_annual", "platform_product_identifier": "annual_sub"}],
              "paywall_components": {
                "components_config": {
                  "background_image": {
                    "light": {
                      "webp": "${baseUrl()}/assets/background.webp",
                      "webp_low_res": "${baseUrl()}/assets/background_low_res.webp"
                    }
                  },
                  "close_icon": {
                    "base_url": "${baseUrl()}/icons",
                    "icon_name": "x",
                    "formats": {"webp": "x.webp", "png": "x.png"}
                  }
                }
              }
            },
            {
              "identifier": "legacy_only",
              "description": "No components paywall",
              "packages": [{"identifier": "${'$'}rc_monthly", "platform_product_identifier": "monthly_sub"}]
            }
          ],
          "ui_config": {"app": {}}
        }
        """.trimIndent(),
    ).toString()

    private fun recorder(refresh: Boolean = false, offeringsFilter: Set<String> = emptySet()) = FixtureRecorder(
        apiKey = "test_api_key",
        baseUrl = baseUrl(),
        appUserId = "test-user",
        offeringsFilter = offeringsFilter,
        outputDirectory = outputDir,
        refresh = refresh,
        log = { },
    )

    @Test
    fun `records filtered offerings json, assets and manifest`() {
        recorder().record()

        val offeringsJson = JSONObject(File(outputDir, "offerings.json").readText())
        val offerings = offeringsJson.getJSONArray("offerings")
        assertEquals(1, offerings.length(), "Offerings without paywall_components should be filtered out")
        assertEquals("default", offerings.getJSONObject(0).getString("identifier"))
        assertEquals("default", offeringsJson.getString("current_offering_id"))
        assertTrue(offeringsJson.has("ui_config"))

        // MockWebServer's host is single-label (localhost or an IP without a meaningful TLD), so assets
        // mirror to their URL path.
        assertTrue(File(outputDir, "assets/background.webp").exists())
        assertTrue(File(outputDir, "assets/background_low_res.webp").exists())
        assertTrue(File(outputDir, "icons/x.webp").exists(), "Icon webp variant should be downloaded")

        val manifest = JSONObject(File(outputDir, "fixture-manifest.json").readText())
        assertEquals(1, manifest.getInt("format_version"))
        assertEquals(1, manifest.getInt("offering_count"))
        assertEquals(3, manifest.getInt("asset_count"))
    }

    @Test
    fun `skips existing assets unless refresh is set`() {
        recorder().record()
        val asset = File(outputDir, "assets/background.webp")
        asset.writeBytes(byteArrayOf(9))

        recorder(refresh = false).record()
        assertEquals(1, asset.length(), "Existing asset should not be re-downloaded")

        recorder(refresh = true).record()
        assertEquals(imageBytes.size.toLong(), asset.length(), "Refresh should re-download the asset")
    }

    @Test
    fun `fails when the offerings filter matches nothing`() {
        val exception = assertFailsWith<IllegalStateException> {
            recorder(offeringsFilter = setOf("nonexistent")).record()
        }
        assertTrue("nonexistent" in exception.message.orEmpty())
    }

    @Test
    fun `mirror path reverses the host without its TLD`() {
        assertEquals(
            "pawwalls/assets/header.webp",
            "https://assets.pawwalls.com/header.webp".toMirrorPath(),
        )
        assertEquals(
            "pawwalls/icons/icons/x.webp",
            "https://icons.pawwalls.com/icons/x.webp".toMirrorPath(),
        )
    }
}
