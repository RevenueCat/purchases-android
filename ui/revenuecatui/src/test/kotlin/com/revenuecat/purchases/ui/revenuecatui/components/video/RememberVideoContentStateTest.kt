package com.revenuecat.purchases.ui.revenuecatui.components.video

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.net.URL

@RunWith(AndroidJUnit4::class)
class RememberVideoContentStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val lightUrl = URL("https://example.com/light.mp4")
    private val darkUrl = URL("https://example.com/dark.mp4")
    private val lightUri = URI("file:///cache/light.mp4")
    private val darkUri = URI("file:///cache/dark.mp4")

    private val repository = FakeFileRepository(cachedFiles = mapOf(lightUrl to lightUri, darkUrl to darkUri))

    @Test
    fun `resolves the new video after state restoration with a different theme`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        // Not a snapshot state on purpose: the restored composition must start with the dark URLs, the way a
        // recreated Activity does, instead of recomposing the live one.
        var videoUrls = videoUrls(lightUrl)
        var resolvedUri: URI? = null

        restorationTester.setContent {
            resolvedUri = rememberVideoContentState(videoUrls, repository)
        }
        composeTestRule.runOnIdle { assertThat(resolvedUri).isEqualTo(lightUri) }

        videoUrls = videoUrls(darkUrl)
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle { assertThat(resolvedUri).isEqualTo(darkUri) }
    }

    private fun videoUrls(url: URL) = VideoUrls(width = 100u, height = 100u, url = url)
}
