package com.revenuecat.purchases.codegen

import kotlin.test.Test
import kotlin.test.assertEquals

class NamingConfigTest {

    // --- toIdentifier: CAMEL_CASE ---

    @Test
    fun `toIdentifier strips rc prefix in camelCase`() {
        assertEquals("monthly", NamingConfig.toIdentifier("\$rc_monthly", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier converts snake to camelCase`() {
        assertEquals("premiumAccess", NamingConfig.toIdentifier("premium_access", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier single word camelCase lowercases entire first segment`() {
        // snakeToCamel lowercases the entire first segment, not just the first char
        assertEquals("prophoto", NamingConfig.toIdentifier("ProPhoto", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier multi-segment camelCase capitalises subsequent words`() {
        assertEquals("premiumAccess", NamingConfig.toIdentifier("premium_access", NamingStyle.CAMEL_CASE))
    }

    // --- toIdentifier: SNAKE_CASE ---

    @Test
    fun `toIdentifier strips rc prefix in snakeCase`() {
        assertEquals("monthly", NamingConfig.toIdentifier("\$rc_monthly", NamingStyle.SNAKE_CASE))
    }

    @Test
    fun `toIdentifier converts camel to snake`() {
        assertEquals("premium_access", NamingConfig.toIdentifier("premiumAccess", NamingStyle.SNAKE_CASE))
    }

    @Test
    fun `toIdentifier converts dash separator to snake`() {
        assertEquals("foo_bar", NamingConfig.toIdentifier("foo-bar", NamingStyle.SNAKE_CASE))
    }

    @Test
    fun `toIdentifier converts space separator to snake`() {
        assertEquals("hello_world", NamingConfig.toIdentifier("hello world", NamingStyle.SNAKE_CASE))
    }

    // --- toIdentifier: AS_IS ---

    @Test
    fun `toIdentifier strips rc prefix in asIs`() {
        assertEquals("monthly", NamingConfig.toIdentifier("\$rc_monthly", NamingStyle.AS_IS))
    }

    @Test
    fun `toIdentifier preserves case in asIs`() {
        assertEquals("ProPhoto", NamingConfig.toIdentifier("ProPhoto", NamingStyle.AS_IS))
    }

    @Test
    fun `toIdentifier asIs sanitizes dots to underscores`() {
        assertEquals("hello_world", NamingConfig.toIdentifier("hello.world", NamingStyle.AS_IS))
    }

    // --- Reserved keywords ---

    @Test
    fun `toIdentifier backtick-wraps reserved keyword class`() {
        assertEquals("`class`", NamingConfig.toIdentifier("class", NamingStyle.AS_IS))
    }

    @Test
    fun `toIdentifier backtick-wraps reserved keyword if`() {
        assertEquals("`if`", NamingConfig.toIdentifier("if", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier backtick-wraps reserved keyword val`() {
        assertEquals("`val`", NamingConfig.toIdentifier("val", NamingStyle.SNAKE_CASE))
    }

    @Test
    fun `toUnescapedIdentifier does not backtick-wrap reserved keyword`() {
        assertEquals("class", NamingConfig.toUnescapedIdentifier("class", NamingStyle.AS_IS))
    }

    @Test
    fun `escapeIfReservedKeyword backtick-wraps reserved keyword`() {
        assertEquals("`class`", NamingConfig.escapeIfReservedKeyword("class"))
    }

    // --- Sanitization ---

    @Test
    fun `toIdentifier prepends underscore for leading digit`() {
        assertEquals("_2pac", NamingConfig.toIdentifier("2pac", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier empty string becomes single underscore`() {
        assertEquals("_", NamingConfig.toIdentifier("", NamingStyle.CAMEL_CASE))
    }

    @Test
    fun `toIdentifier rc prefix only becomes single underscore`() {
        // "$rc_" → stripped to "" → sanitized to "_"
        assertEquals("_", NamingConfig.toIdentifier("\$rc_", NamingStyle.CAMEL_CASE))
    }

    // --- toConstant ---

    @Test
    fun `toConstant simple snake case`() {
        assertEquals("PREMIUM_ACCESS", NamingConfig.toConstant("premium_access"))
    }

    @Test
    fun `toConstant strips rc prefix`() {
        assertEquals("MONTHLY", NamingConfig.toConstant("\$rc_monthly"))
    }

    @Test
    fun `toConstant leading digit gets underscore prefix`() {
        assertEquals("_2PAC", NamingConfig.toConstant("2pac"))
    }

    @Test
    fun `toConstant replaces dashes with underscores`() {
        assertEquals("HELLO_WORLD", NamingConfig.toConstant("hello-world"))
    }

    @Test
    fun `toConstant single word uppercased`() {
        assertEquals("PREMIUM", NamingConfig.toConstant("premium"))
    }

    @Test
    fun `toConstant already uppercase preserved`() {
        assertEquals("ANNUAL", NamingConfig.toConstant("ANNUAL"))
    }
}
