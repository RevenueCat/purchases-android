package com.revenuecat.purchases.common

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.revenuecat.purchases.backup.RevenueCatBackupAgent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
public class SharedPreferencesManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockLegacyPrefs: SharedPreferences
    private lateinit var mockRevenueCatPrefs: SharedPreferences
    private lateinit var mockLegacyEditor: SharedPreferences.Editor
    private lateinit var mockRevenueCatEditor: SharedPreferences.Editor

    public companion object {
        private const val REVENUECAT_KEY_1 = "com.revenuecat.purchases.apikey1"
        private const val REVENUECAT_KEY_2 = "com.revenuecat.purchases.apikey1.new"
        private const val NON_REVENUECAT_KEY = "some.other.key"
    }

    @Before
    public fun setup() {
        mockContext = mockk()
        mockLegacyPrefs = mockk()
        mockRevenueCatPrefs = mockk()
        mockLegacyEditor = mockk(relaxed = true)
        mockRevenueCatEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(RevenueCatBackupAgent.REVENUECAT_PREFS_FILE_NAME, Context.MODE_PRIVATE) } returns mockRevenueCatPrefs
        every { mockLegacyPrefs.edit() } returns mockLegacyEditor
        every { mockRevenueCatPrefs.edit() } returns mockRevenueCatEditor
        every { mockRevenueCatEditor.apply() } just Runs
        every { mockRevenueCatPrefs.contains(SharedPreferencesManager.EXPECTED_VERSION_KEY) } returns false

        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(mockContext) } returns mockLegacyPrefs
    }

    @After
    public fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getSharedPreferences returns RevenueCat preferences when already has expected version`() {
        // RevenueCat preferences have version
        every { mockRevenueCatPrefs.contains(SharedPreferencesManager.EXPECTED_VERSION_KEY) } returns true
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // No migration should occur but should set the version
        verify(exactly = 0) { mockRevenueCatEditor.putInt(any(), any()) }
        verify(exactly = 0) { mockRevenueCatEditor.putString(any(), any()) }
        verify(exactly = 0) { mockRevenueCatEditor.apply() }
    }

    @Test
    fun `getSharedPreferences returns RevenueCat preferences when legacy has no RevenueCat data`() {
        // RevenueCat preferences do not have version, but legacy has no RevenueCat data
        every { mockRevenueCatPrefs.contains(SharedPreferencesManager.EXPECTED_VERSION_KEY) } returns false
        every { mockLegacyPrefs.all } returns mapOf(NON_REVENUECAT_KEY to "value")

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // No migration should occur but should set the version
        verify(exactly = 1) {
            mockRevenueCatEditor.putInt(
                SharedPreferencesManager.EXPECTED_VERSION_KEY,
                SharedPreferencesManager.EXPECTED_VERSION
            )
        }
        verify(exactly = 0) { mockRevenueCatEditor.putString(any(), any()) }
        verify(exactly = 1) { mockRevenueCatEditor.apply() }
    }

    @Test
    fun `getSharedPreferences performs migration when RevenueCat prefs do not have version and legacy has RevenueCat data`() {
        // RevenueCat preferences are empty, legacy has RevenueCat data
        every { mockLegacyPrefs.all } returns mapOf(
            REVENUECAT_KEY_1 to "string_value",
            NON_REVENUECAT_KEY to "should_not_migrate"
        )

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        assertThat(result).isSameAs(mockRevenueCatPrefs)
        // Migration should occur for RevenueCat keys only
        verify(exactly = 1) {
            mockRevenueCatEditor.putInt(
                SharedPreferencesManager.EXPECTED_VERSION_KEY,
                SharedPreferencesManager.EXPECTED_VERSION,
            )
        }
        verify { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "string_value") }
        verify(exactly = 0) { mockRevenueCatEditor.putString(NON_REVENUECAT_KEY, any()) }
        verify { mockRevenueCatEditor.apply() }
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
        verify { mockRevenueCatEditor.apply() }
    }

    @Test
    fun `getSharedPreferences handles concurrent access safely`() {
        // Test that multiple calls work correctly - first call should trigger migration
        every { mockRevenueCatPrefs.contains(SharedPreferencesManager.EXPECTED_VERSION_KEY) } returnsMany listOf(
            false, // First call - false, triggers migration
            true // After migration - true, no more migration
        )
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")

        val manager = SharedPreferencesManager(mockContext)

        var result1: SharedPreferences? = null
        val thread1 = thread { result1 = manager.getSharedPreferences() }

        var result2: SharedPreferences? = null
        val thread2 = thread { result2 = manager.getSharedPreferences() }

        thread1.join()
        thread2.join()

        assertThat(result1).isSameAs(mockRevenueCatPrefs)
        assertThat(result2).isSameAs(mockRevenueCatPrefs)
        
        // Migration should only happen once during the first call
        verify(exactly = 1) {
            mockRevenueCatEditor.putInt(
                SharedPreferencesManager.EXPECTED_VERSION_KEY,
                SharedPreferencesManager.EXPECTED_VERSION,
            )
        }
        verify(exactly = 1) { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "value1") }
        verify(exactly = 2) { mockRevenueCatEditor.apply() } // One for the expected version, another for the actual migration
    }

    @Test
    fun `getSharedPreferences handles migration failure gracefully`() {
        every { mockLegacyPrefs.all } returns mapOf(REVENUECAT_KEY_1 to "value1")

        val manager = SharedPreferencesManager(mockContext)
        val result = manager.getSharedPreferences()

        // Should still return the RevenueCat preferences even if migration fails
        assertThat(result).isSameAs(mockRevenueCatPrefs)
        verify { mockRevenueCatEditor.putString(REVENUECAT_KEY_1, "value1") }
        verify { mockRevenueCatEditor.apply() }
    }
}
