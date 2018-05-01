package com.revenuecat.purchases;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
    private final String apiKey = "api_key";
    private final String appUserID = "app_user_id";
    private String cacheKey = apiKey + "_" + appUserID;

    @Before
    public void setup() {
        mockPrefs = mock(SharedPreferences.class);

        cache = new PurchaserInfoCache(mockPrefs, appUserID, apiKey);
    }

    @Test
    public void canCreateCache() {
        assertNotNull(cache);
    }

    @Test
    public void returnsNullIfNoCachedInfo() {
        when(mockPrefs.getString(any(String.class), (String) eq(null))).thenReturn(null);
        PurchaserInfo info = cache.getCachedPurchaserInfo();
        assertNull(info);
    }

    @Test
    public void checksCorrectCacheKey() {
        PurchaserInfo info = cache.getCachedPurchaserInfo();
        verify(mockPrefs).getString(eq(cacheKey), (String) eq(null));
    }
}
