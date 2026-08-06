package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PrewarmBridgeCallbacksTest {

    private val callbacks = PrewarmBridgeCallbacks()

    private fun rebind(
        onContentResize: (Int?, Int?) -> Unit = { _, _ -> },
        onLoadFailed: () -> Unit = {},
    ) = callbacks.rebind(onContentResize, onDocumentReset = {}, onLoadFailed = onLoadFailed)

    @Test
    fun `replays the last content size delivered before adoption`() {
        callbacks.dispatchResize(320, 240)

        var width: Int? = null
        var height: Int? = null
        rebind(onContentResize = { w, h -> width = w; height = h })

        assertThat(width).isEqualTo(320)
        assertThat(height).isEqualTo(240)
    }

    @Test
    fun `replays each axis independently when only one was reported`() {
        callbacks.dispatchResize(null, 240)
        callbacks.dispatchResize(320, null)

        var width: Int? = null
        var height: Int? = null
        rebind(onContentResize = { w, h -> width = w; height = h })

        assertThat(width).isEqualTo(320)
        assertThat(height).isEqualTo(240)
    }

    @Test
    fun `does not replay a size when none was reported`() {
        var resized = false
        rebind(onContentResize = { _, _ -> resized = true })

        assertThat(resized).isFalse()
    }

    @Test
    fun `forwards resizes to the bound handler after adoption`() {
        var width: Int? = null
        rebind(onContentResize = { w, _ -> width = w })

        callbacks.dispatchResize(500, null)

        assertThat(width).isEqualTo(500)
    }

    @Test
    fun `document reset clears the cached size so it is not replayed`() {
        callbacks.dispatchResize(320, 240)
        callbacks.dispatchDocumentReset()

        var resized = false
        rebind(onContentResize = { _, _ -> resized = true })

        assertThat(resized).isFalse()
    }

    @Test
    fun `replays a terminal failure delivered before adoption`() {
        callbacks.dispatchLoadFailed()

        var failed = false
        rebind(onLoadFailed = { failed = true })

        assertThat(failed).isTrue()
    }

    @Test
    fun `does not replay a stale size once the load has failed`() {
        callbacks.dispatchResize(320, 240)
        callbacks.dispatchLoadFailed()

        var resized = false
        var failed = false
        rebind(onContentResize = { _, _ -> resized = true }, onLoadFailed = { failed = true })

        assertThat(failed).isTrue()
        assertThat(resized).isFalse()
    }
}
