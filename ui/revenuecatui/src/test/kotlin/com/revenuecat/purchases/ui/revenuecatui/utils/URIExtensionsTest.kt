package com.revenuecat.purchases.ui.revenuecatui.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class URIExtensionsTest {

    @Test
    fun `appendQueryParameter appends parameter correctly to URI without existing query`() {
        val uri = URI("https://example.com/path")
        val updatedUri = uri.appendQueryParameter("key", "value")
        assertEquals(updatedUri.toString(), "https://example.com/path?key=value")
    }

    @Test
    fun `appendQueryParameter appends parameter correctly to URI with existing query`() {
        val uri = URI("https://example.com/path?existing=param")
        val updatedUri = uri.appendQueryParameter("key", "value")
        assertEquals(updatedUri.toString(), "https://example.com/path?existing=param&key=value")
    }

    @Test
    fun `appendQueryParameter works with custom URI schemes`() {
        val uri = URI("revenuecatbilling://test")
        val updatedUri = uri.appendQueryParameter("key", "value")
        assertEquals(updatedUri.toString(), "revenuecatbilling://test?key=value")
    }

    @Test
    fun `appendQueryParameter works with custom URI schemes and existing query`() {
        val uri = URI("revenuecatbilling://test?rc_source=app")
        val updatedUri = uri.appendQueryParameter("key", "value")
        assertEquals(updatedUri.toString(), "revenuecatbilling://test?rc_source=app&key=value")
    }
}
