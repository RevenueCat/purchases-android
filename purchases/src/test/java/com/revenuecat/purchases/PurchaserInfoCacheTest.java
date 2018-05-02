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
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchaserInfoCacheTest {

    private PurchaserInfoCache cache;
    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private final String apiKey = "api_key";
    private final String appUserID = "app_user_id";
    private String cacheKey = apiKey + "_" + appUserID;

    @Before
    public void setup() {
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        when(mockEditor.putString(any(String.class), any(String.class))).thenReturn(mockEditor);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        cache = new PurchaserInfoCache(mockPrefs, apiKey);
    }

    @Test
    public void canCreateCache() {
        assertNotNull(cache);
    }

    @Test
    public void returnsNullIfNoCachedInfo() {
        when(mockPrefs.getString(any(String.class), (String) eq(null))).thenReturn(null);
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNull(info);
    }

    @Test
    public void checksCorrectCacheKey() {
        cache.getCachedPurchaserInfo(appUserID);
        verify(mockPrefs).getString(eq(cacheKey), (String) eq(null));
    }

    @Test
    public void parsesJSONObject() {
        when(mockPrefs.getString(eq(cacheKey), (String) eq(null)))
                .thenReturn(validFullPurchaserResponse);
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNotNull(info);
    }

    @Test
    public void returnsNullForInvalidJSON() {
        when(mockPrefs.getString(eq(cacheKey), (String) eq(null)))
                .thenReturn("not json");
        PurchaserInfo info = cache.getCachedPurchaserInfo(appUserID);
        assertNull(info);
    }

    @Test
    public void canCachePurchaserInfo() throws JSONException {
        JSONObject jsonObject = new JSONObject(validFullPurchaserResponse);
        PurchaserInfo info = new PurchaserInfo.Factory().build(jsonObject);

        cache.cachePurchaserInfo(appUserID, info);

        verify(mockEditor).putString(eq(cacheKey), any(String.class));
        verify(mockEditor).apply();
    }
}
