package com.revenuecat.purchases.ui.revenuecatui.utils

import org.junit.Test

class URLExtensionsTest {

    @Test
    fun `appendQueryParameter appends parameter correctly to URL without existing query`() {
        val url = java.net.URL("https://example.com/path")
        val updatedUrl = url.appendQueryParameter("key", "value")
        assert(updatedUrl.toString() == "https://example.com/path?key=value")
    }

    @Test
    fun `appendQueryParameter encodes query parameter name and value`() {
        val url = java.net.URL("https://example.com/path")
        val updatedUrl = url.appendQueryParameter("package name", "Annual Trial & Intro")
        assert(updatedUrl.toString() == "https://example.com/path?package%20name=Annual%20Trial%20%26%20Intro")
    }

    @Test
    fun `appendQueryParameter appends parameter correctly to URL with existing query`() {
        val url = java.net.URL("https://example.com/path?existing=param")
        val updatedUrl = url.appendQueryParameter("key", "value")
        assert(updatedUrl.toString() == "https://example.com/path?existing=param&key=value")
    }

    @Test
    fun `appendQueryParameter appends parameter before fragment`() {
        val url = java.net.URL("https://example.com/path?existing=param#section")
        val updatedUrl = url.appendQueryParameter("key", "value")
        assert(updatedUrl.toString() == "https://example.com/path?existing=param&key=value#section")
    }
}
