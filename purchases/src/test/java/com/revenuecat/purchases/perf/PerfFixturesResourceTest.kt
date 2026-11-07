package com.revenuecat.purchases.perf

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PerfFixturesResourceTest {
    @Test
    fun everyManifestEntryResolvesToNonEmptyJson() {
        val entries = PerfFixtures.loadManifest()
        assertThat(entries).isNotEmpty
        entries.forEach { entry ->
            val body = PerfFixtures.readBody(entry.file)
            assertThat(body).isNotBlank
            // parses as JSON object/array
            assertThat(body.trimStart().first()).isIn('{', '[')
        }
    }
}
