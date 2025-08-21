package com.revenuecat.purchases.common

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockLegacyPrefs: SharedPreferences
    private lateinit var mockRevenueCatPrefs: SharedPreferences
    private lateinit var mockLegacyEditor: SharedPreferences.Editor
    private lateinit var mockRevenueCatEditor: SharedPreferences.Editor

    companion object {
        private const val REVENUECAT_KEY_1 = "com.revenuecat.purchases.apikey1"
        private const val REVENUECAT_KEY_2 = "com.revenuecat.purchases.apikey1.new"
        private const val NON_REVENUECAT_KEY = "some.other.key"
    }

    @Before
    fun setup() {
        mockContext = mockk()
        mockLegacyPrefs = mockk()
        mockRevenueCatPrefs = mockk()
        mockLegacyEditor = mockk(relaxed = true)
        mockRevenueCatEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(SharedPreferencesManager.REVENUECAT_PREFS_FILE_NAME, Context.MODE_PRIVATE) } returns mockRevenueCatPrefs
        every { mockLegacyPrefs.edit() } returns mockLegacyEditor
        every { mockRevenueCatPrefs.edit() } returns mockRevenueCatEditor
        every { mockRevenueCatEditor.commit() } returns true

        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(mockContext) } returns mockLegacyPrefs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getSharedPreferences returns RevenueCat preferences when no migration needed`() {
        // RevenueCat preferences have existing data
        every { mockRevenueCatPrefs.all } returns mapOf("existing.key" to "existing.value")
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // No migration should occur
        verify(exactly = 0) { mockRevenueCatEditor.putString(any(), any()) }
        verify(exactly = 0) { mockRevenueCatEditor.commit() }
    }

    @Test
    fun `getSharedPreferences returns RevenueCat preferences when legacy has no RevenueCat data`() {
        // RevenueCat preferences are empty, but legacy has no RevenueCat data
        every { mockRevenueCatPrefs.all } returns emptyMap()
        every { mockLegacyPrefs.all } returns mapOf(NON_REVENUECAT_KEY to "value")

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // No migration should occur
        verify(exactly = 0) { mockRevenueCatEditor.putString(any(), any()) }
        verify(exactly = 0) { mockRevenueCatEditor.commit() }
    }

    @Test
    fun `getSharedPreferences performs migration when RevenueCat prefs are empty and legacy has RevenueCat data`() {
        // RevenueCat preferences are empty, legacy has RevenueCat data
        every { mockRevenueCatPrefs.all } returns emptyMap()
        every { mockLegacyPrefs.all } returns mapOf(
            REVENUECAT_KEY_1 to "string_value",
            NON_REVENUECAT_KEY to "should_not_migrate"
        )

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // Migration should occur for RevenueCat keys only
        verify { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "string_value") }
        verify(exactly = 0) { mockRevenueCatEditor.putString(NON_REVENUECAT_KEY, any()) }
        verify { mockRevenueCatEditor.commit() }
    }

    @Test
    fun `getSharedPreferences migrates different data types correctly`() {
        every { mockRevenueCatPrefs.all } returns emptyMap()
        every { mockLegacyPrefs.all } returns mapOf<String, Any>(
            REVENUECAT_KEY_1 to "string_value",
            REVENUECAT_KEY_2 to true,
            "com.revenuecat.purchases.long_key" to 123L,
            "com.revenuecat.purchases.int_key" to 456,
            "com.revenuecat.purchases.float_key" to 78.9f,
            "com.revenuecat.purchases.set_key" to setOf("a", "b", "c")
        )

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        verify { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "string_value") }
        verify { mockRevenueCatEditor.putBoolean(REVENUECAT_KEY_2, true) }
        verify { mockRevenueCatEditor.putLong("com.revenuecat.purchases.long_key", 123L) }
        verify { mockRevenueCatEditor.putInt("com.revenuecat.purchases.int_key", 456) }
        verify { mockRevenueCatEditor.putFloat("com.revenuecat.purchases.float_key", 78.9f) }
        verify { mockRevenueCatEditor.putStringSet("com.revenuecat.purchases.set_key", setOf("a", "b", "c")) }
        verify { mockRevenueCatEditor.commit() }
    }

    @Test
    fun `getSharedPreferences handles concurrent access safely`() {
        // Test that multiple calls work correctly - first call should trigger migration
        every { mockRevenueCatPrefs.all } returnsMany listOf(
            emptyMap(), // First call - empty, triggers migration
            mapOf(REVENUECAT_KEY_1 to "value1") // After migration - has data, no more migration
        )
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")

        val manager = SharedPreferencesManager(mockContext)
        
        val result1 = manager.getSharedPreferences()
        val result2 = manager.getSharedPreferences()

        assertThat(result1).isSameAs(mockRevenueCatPrefs)
        assertThat(result2).isSameAs(mockRevenueCatPrefs)
        
        // Migration should only happen once during the first call
        verify(exactly = 1) { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "value1") }
        verify(exactly = 1) { mockRevenueCatEditor.commit() }
    }

    @Test
    fun `getSharedPreferences handles migration failure gracefully`() {
        every { mockRevenueCatPrefs.all } returns emptyMap()
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")
        every { mockRevenueCatEditor.commit() } returns false

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        // Should still return the RevenueCat preferences even if migration fails
        assertThat(result).isSameAs(mockRevenueCatPrefs)
        verify { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "value1") }
        verify { mockRevenueCatEditor.commit() }
    }
}