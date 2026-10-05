package com.revenuecat.purchases.common.sdksettings

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class SdkSettingsTest {

    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `parses the diagnostics section`() {
        assertThat(parse("""{"diagnostics":{"enabled":true}}"""))
            .isEqualTo(SdkSettings(diagnostics = DiagnosticsSettings(enabled = true)))
        assertThat(parse("""{"diagnostics":{"enabled":false}}"""))
            .isEqualTo(SdkSettings(diagnostics = DiagnosticsSettings(enabled = false)))
    }

    @Test
    fun `an empty item is the default settings`() {
        assertThat(parse("{}")).isEqualTo(SdkSettings.DEFAULT)
        assertThat(SdkSettings.DEFAULT.diagnostics).isNull()
    }

    @Test
    fun `unknown sections and unknown keys are ignored`() {
        assertThat(parse("""{"future_setting":{"x":1},"diagnostics":{"enabled":true,"future_key":"y"}}"""))
            .isEqualTo(SdkSettings(diagnostics = DiagnosticsSettings(enabled = true)))
    }

    @Test
    fun `a diagnostics section without enabled leaves enabled undecided`() {
        assertThat(parse("""{"diagnostics":{}}"""))
            .isEqualTo(SdkSettings(diagnostics = DiagnosticsSettings(enabled = null)))
    }

    @Test
    fun `a diagnostics section that is not an object is dropped`() {
        assertThat(parse("""{"diagnostics":"yes"}""")).isEqualTo(SdkSettings.DEFAULT)
        assertThat(parse("""{"diagnostics":[true]}""")).isEqualTo(SdkSettings.DEFAULT)
    }

    @Test
    fun `a diagnostics section with a mistyped enabled is dropped`() {
        assertThat(parse("""{"diagnostics":{"enabled":"true"}}""")).isEqualTo(SdkSettings.DEFAULT)
    }

    private fun parse(json: String): SdkSettings = SdkSettings.parse(Json.parseToJsonElement(json).jsonObject)
}
