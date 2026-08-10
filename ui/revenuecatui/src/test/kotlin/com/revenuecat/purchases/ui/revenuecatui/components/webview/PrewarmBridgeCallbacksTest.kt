package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PrewarmBridgeCallbacksTest {

    private val callbacks = PrewarmBridgeCallbacks()

    private fun rebind(
        onContentResize: (Int?, Int?) -> Unit = { _, _ -> },
        onDocumentReset: () -> Unit = {},
        onLoadFailed: () -> Unit = {},
    ) = callbacks.rebind(onContentResize, onDocumentReset = onDocumentReset, onLoadFailed = onLoadFailed)

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

    // A prewarm-time failure must not become the adopting component's failure: it is recorded so the
    // prewarmer can refuse the entry, and the display path then loads cold.
    @Test
    fun `records a terminal failure without replaying it`() {
        callbacks.dispatchLoadFailed()

        var failed = false
        rebind(onLoadFailed = { failed = true })

        assertThat(callbacks.loadFailed).isTrue()
        assertThat(failed).isFalse()
    }

    @Test
    fun `forwards a terminal failure that arrives after adoption`() {
        var failed = false
        rebind(onLoadFailed = { failed = true })

        callbacks.dispatchLoadFailed()

        assertThat(failed).isTrue()
    }

    @Test
    fun `keeps the replayed size across the activation navigation`() {
        callbacks.dispatchResize(320, 240)

        var width: Int? = null
        var reset = false
        rebind(onContentResize = { w, _ -> width = w }, onDocumentReset = { reset = true })
        callbacks.ignoreDocumentResetFromActivation()
        callbacks.dispatchDocumentReset()

        assertThat(width).isEqualTo(320)
        assertThat(reset).isFalse()
    }

    @Test
    fun `only the activation navigation is ignored`() {
        callbacks.dispatchResize(320, 240)
        callbacks.ignoreDocumentResetFromActivation()
        callbacks.dispatchDocumentReset()

        var reset = false
        rebind(onDocumentReset = { reset = true })
        callbacks.dispatchDocumentReset()

        assertThat(reset).isTrue()
    }
}
