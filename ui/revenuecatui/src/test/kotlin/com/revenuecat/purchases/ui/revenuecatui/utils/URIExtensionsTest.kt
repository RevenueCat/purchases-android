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
    fun `appendQueryParameter encodes query parameter name and value`() {
        val uri = URI("https://example.com/path")
        val updatedUri = uri.appendQueryParameter("package name", "Annual Trial & Intro")
        assertEquals(updatedUri.toString(), "https://example.com/path?package%20name=Annual%20Trial%20%26%20Intro")
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

    @Test
    fun `appendQueryParameter appends parameter before fragment`() {
        val uri = URI("https://example.com/path?existing=param#section")
        val updatedUri = uri.appendQueryParameter("key", "value")
        assertEquals(updatedUri.toString(), "https://example.com/path?existing=param&key=value#section")
    }
}
