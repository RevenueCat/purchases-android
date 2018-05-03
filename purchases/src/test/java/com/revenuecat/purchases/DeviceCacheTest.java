package com.revenuecat.purchases;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.revenuecat.purchases.PurchaserInfoTest.validFullPurchaserResponse;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DeviceCacheTest {

    private DeviceCache cache;
    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private final String apiKey = "api_key";
    private final String appUserID = "app_user_id";
    private final String userIDCacheKey = "com.revenuecat.purchases." + apiKey;
    private final String purchaserInfoCacheKey = userIDCacheKey + "." + appUserID;

    @Before
    public void setup() {
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        when(mockEditor.putString(any(String.class), any(String.class))).thenReturn(mockEditor);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        cache = new DeviceCache(mockPrefs, apiKey);
    }

    private void mockString(String key, String value) {
        when(mockPrefs.getString(eq(key), (String) eq(null))).thenReturn(value);
    }

    @Test
    public void canCreateCache() {
        assertNotNull(cache);
    }

    @Test
    public void returnsNullIfNoCachedInfo() {
        mockString(purchaserInfoCacheKey, null);
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNull(info);
    }

    @Test
    public void checksCorrectCacheKey() {
        cache.getCachedPurchaserInfo(appUserID);
        verify(mockPrefs).getString(eq(purchaserInfoCacheKey), (String) eq(null));
    }

    @Test
    public void parsesJSONObject() {
        mockString(purchaserInfoCacheKey, validFullPurchaserResponse);
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNotNull(info);
    }

    @Test
    public void returnsNullForInvalidJSON() {
        mockString(purchaserInfoCacheKey, "not json");
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNull(info);
    }

    @Test
    public void canCachePurchaserInfo() throws JSONException {
        JSONObject jsonObject = new JSONObject(validFullPurchaserResponse);
        PurchaserInfo info = new PurchaserInfo.Factory().build(jsonObject);

        cache.cachePurchaserInfo(appUserID, info);

        verify(mockEditor).putString(eq(purchaserInfoCacheKey), any(String.class));
        verify(mockEditor).apply();
    }

    @Test
    public void returnsNullIfNoAppUserID() {
        String appUserID = cache.getCachedAppUserID();
        assertNull(appUserID);
    }

    @Test
    public void returnsAppUserID() {
        mockString(userIDCacheKey, appUserID);
        String appUserID = cache.getCachedAppUserID();
        assertEquals(this.appUserID, appUserID);
    }

    @Test
    public void canCacheAppUserID() {
        cache.cacheAppUserID(appUserID);
        verify(mockEditor).putString(eq(userIDCacheKey), any(String.class));
    }
}
