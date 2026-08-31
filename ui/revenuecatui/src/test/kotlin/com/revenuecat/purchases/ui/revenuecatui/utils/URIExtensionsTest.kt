package com.revenuecat.purchases.ui.revenuecatui.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URI

class URIExtensionsTest {

    @Test
    fun `upsertQueryParameters returns the URI untouched when there are no parameters`() {
        val uri = URI("https://example.com/path")

        assertThat(uri.upsertQueryParameters(emptyMap())).isEqualTo(uri)
    }

    @Test
    fun `upsertQueryParameters adds parameters to a URI without existing query`() {
        val uri = URI("https://example.com/path")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "value", "other" to "thing"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?key=value&other=thing")
    }

    @Test
    fun `upsertQueryParameters encodes parameter names and values`() {
        val uri = URI("https://example.com/path")

        val updatedUri = uri.upsertQueryParameters(mapOf("package name" to "Annual Trial & Intro"))

        assertThat(updatedUri.toString())
            .isEqualTo("https://example.com/path?package%20name=Annual%20Trial%20%26%20Intro")
    }

    @Test
    fun `upsertQueryParameters keeps unrelated parameters and appends new ones`() {
        val uri = URI("https://example.com/path?existing=param")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "value"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?existing=param&key=value")
    }

    @Test
    fun `upsertQueryParameters replaces an existing parameter where it is`() {
        val uri = URI("https://example.com/path?first=1&key=old&last=2")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "new"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?first=1&key=new&last=2")
    }

    @Test
    fun `upsertQueryParameters drops duplicates of a replaced parameter`() {
        val uri = URI("https://example.com/path?key=old&keep=1&key=older")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "new"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?key=new&keep=1")
    }

    @Test
    fun `upsertQueryParameters matches existing parameters by decoded name`() {
        val uri = URI("https://example.com/path?package%20name=old")

        val updatedUri = uri.upsertQueryParameters(mapOf("package name" to "new"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?package%20name=new")
    }

    @Test
    fun `upsertQueryParameters adds parameters before the fragment`() {
        val uri = URI("https://example.com/path?existing=param#section")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "value"))

        assertThat(updatedUri.toString()).isEqualTo("https://example.com/path?existing=param&key=value#section")
    }

    @Test
    fun `upsertQueryParameters works with custom URI schemes`() {
        val uri = URI("revenuecatbilling://test?rc_source=dashboard")

        val updatedUri = uri.upsertQueryParameters(mapOf("rc_source" to "app", "key" to "value"))

        assertThat(updatedUri.toString()).isEqualTo("revenuecatbilling://test?rc_source=app&key=value")
    }

    @Test
    fun `upsertQueryParameters keeps the query of opaque URIs`() {
        val uri = URI("merchant:checkout?campaign=summer#details")

        val updatedUri = uri.upsertQueryParameters(mapOf("key" to "value"))

        assertThat(updatedUri.toString()).isEqualTo("merchant:checkout?campaign=summer&key=value#details")
    }
}
