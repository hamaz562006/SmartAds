package com.partharoypc.smartads;

import com.partharoypc.smartads.analytics.SmartAdsAdjustAdapter;
import com.partharoypc.smartads.managers.AdFrequencyManager;
import com.partharoypc.smartads.ui.NativeAdBinder;
import com.partharoypc.smartads.utils.SmartAdsPreloadHelper;
import com.partharoypc.smartads.utils.SmartAdsRemoteConfigMapper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExampleUnitTest {

    @Test
    public void testConfigBuilderWithNewFeatures() {
        SmartAdsConfig config = new SmartAdsConfig.Builder()
                .setAdsEnabled(true)
                .setPremium(false)
                .setRewardedInterstitialEnabled(true)
                .setAdMobRewardedInterstitialId("test_rewarded_interstitial_id")
                .setCollapsibleBannerPosition("top")
                .setCollapsibleBannerEnabled(true)
                .setInterstitialIntervalSeconds(45)
                .setDelayAfterRewardedSeconds(60)
                .setAppOpenIntervalSeconds(20)
                .addScreenPreloadRule("MainActivity", "INTERSTITIAL,REWARDED")
                .build();

        assertTrue(config.isAdsEnabled());
        assertFalse(config.isPremium());
        assertTrue(config.isRewardedInterstitialEnabled());
        assertEquals("test_rewarded_interstitial_id", config.getAdMobRewardedInterstitialId());
        assertEquals("top", config.getCollapsibleBannerPosition());
        assertTrue(config.isCollapsibleBannerEnabled());
        assertEquals(45L, config.getInterstitialIntervalSeconds());
        assertEquals(60L, config.getDelayAfterRewardedSeconds());
        assertEquals(20L, config.getAppOpenIntervalSeconds());
        assertEquals("INTERSTITIAL,REWARDED", config.getScreenPreloadRules().get("MainActivity"));
    }

    @Test
    public void testRemoteConfigMapper() {
        Map<String, Object> remoteMap = new HashMap<>();
        remoteMap.put("ads_enabled", true);
        remoteMap.put("is_premium", true);
        remoteMap.put("rewarded_interstitial_enabled", true);
        remoteMap.put("admob_rewarded_interstitial_id", "ca-app-pub-test/123");
        remoteMap.put("collapsible_banner_position", "bottom");
        remoteMap.put("interstitial_interval_seconds", 50);
        remoteMap.put("delay_after_rewarded_seconds", 40);
        remoteMap.put("app_open_interval_seconds", 25);
        remoteMap.put("PRELOAD_HomeFragment", "INTERSTITIAL");

        SmartAdsConfig.Builder builder = new SmartAdsConfig.Builder();
        SmartAdsRemoteConfigMapper.applyRemoteConfig(builder, remoteMap);
        SmartAdsConfig config = builder.build();

        assertTrue(config.isAdsEnabled());
        assertTrue(config.isPremium());
        assertTrue(config.isRewardedInterstitialEnabled());
        assertEquals("ca-app-pub-test/123", config.getAdMobRewardedInterstitialId());
        assertEquals("bottom", config.getCollapsibleBannerPosition());
        assertEquals(50L, config.getInterstitialIntervalSeconds());
        assertEquals(40L, config.getDelayAfterRewardedSeconds());
        assertEquals(25L, config.getAppOpenIntervalSeconds());
        assertEquals("INTERSTITIAL", config.getScreenPreloadRules().get("HomeFragment"));
    }

    @Test
    public void testConfigToBuilderPreservesFields() {
        SmartAdsConfig original = new SmartAdsConfig.Builder()
                .setRewardedInterstitialEnabled(true)
                .setAdMobRewardedInterstitialId("ri_123")
                .setCollapsibleBannerPosition("top")
                .setPremium(true)
                .setInterstitialIntervalSeconds(35)
                .setDelayAfterRewardedSeconds(45)
                .setAppOpenIntervalSeconds(15)
                .build();

        SmartAdsConfig cloned = original.toBuilder().build();

        assertEquals(original.isRewardedInterstitialEnabled(), cloned.isRewardedInterstitialEnabled());
        assertEquals(original.getAdMobRewardedInterstitialId(), cloned.getAdMobRewardedInterstitialId());
        assertEquals(original.getCollapsibleBannerPosition(), cloned.getCollapsibleBannerPosition());
        assertEquals(original.isPremium(), cloned.isPremium());
        assertEquals(original.getInterstitialIntervalSeconds(), cloned.getInterstitialIntervalSeconds());
        assertEquals(original.getDelayAfterRewardedSeconds(), cloned.getDelayAfterRewardedSeconds());
        assertEquals(original.getAppOpenIntervalSeconds(), cloned.getAppOpenIntervalSeconds());
    }

    @Test
    public void testAdFrequencyManager() {
        AdFrequencyManager manager = AdFrequencyManager.getInstance();
        manager.reset();

        // Initially no ads shown -> allowed
        assertTrue(manager.canShowInterstitial(30, 30));
        assertTrue(manager.canShowAppOpen(15));

        // Record Interstitial shown
        manager.recordInterstitialShown();
        // Immediately after, interstitial is capped
        assertFalse(manager.canShowInterstitial(30, 30));

        // Reset and test rewarded cooldown
        manager.reset();
        assertTrue(manager.canShowInterstitial(30, 30));
        manager.recordRewardedShown();
        // Post-rewarded cooldown blocks interstitial
        assertFalse(manager.canShowInterstitial(30, 30));

        // App Open test
        manager.reset();
        assertTrue(manager.canShowAppOpen(15));
        manager.recordAppOpenShown();
        assertFalse(manager.canShowAppOpen(15));
    }

    @Test
    public void testNativeAdBinder() {
        NativeAdBinder binder = new NativeAdBinder.Builder(1001)
                .setHeadlineViewId(1002)
                .setBodyViewId(1003)
                .setCallToActionViewId(1004)
                .setIconViewId(1005)
                .setMediaViewId(1006)
                .setAdvertiserViewId(1007)
                .setPriceViewId(1008)
                .setStoreViewId(1009)
                .setStarRatingViewId(1010)
                .build();

        assertEquals(1001, binder.getLayoutRes());
        assertEquals(1002, binder.getHeadlineViewId());
        assertEquals(1003, binder.getBodyViewId());
        assertEquals(1004, binder.getCallToActionViewId());
        assertEquals(1005, binder.getIconViewId());
        assertEquals(1006, binder.getMediaViewId());
        assertEquals(1007, binder.getAdvertiserViewId());
        assertEquals(1008, binder.getPriceViewId());
        assertEquals(1009, binder.getStoreViewId());
        assertEquals(1010, binder.getStarRatingViewId());
    }

    @Test
    public void testAdjustAdapterSafeFallback() {
        SmartAdsAdjustAdapter adapter = new SmartAdsAdjustAdapter("admob_sdk");
        assertNotNull(adapter);
        // Track revenue without crashing even when Adjust SDK is not on classpath
        adapter.trackAdRevenue("test_unit", "INTERSTITIAL", "Google AdMob", 1500000L, "USD", 1);
    }
}