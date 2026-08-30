package com.partharoypc.smartads;

import android.os.Bundle;

import com.partharoypc.smartads.utils.SmartAdsAnalyticsAdapter;
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
                .setRewardedInterstitialEnabled(true)
                .setAdMobRewardedInterstitialId("test_rewarded_interstitial_id")
                .setCollapsibleBannerPosition("top")
                .setCollapsibleBannerEnabled(true)
                .build();

        assertTrue(config.isAdsEnabled());
        assertTrue(config.isRewardedInterstitialEnabled());
        assertEquals("test_rewarded_interstitial_id", config.getAdMobRewardedInterstitialId());
        assertEquals("top", config.getCollapsibleBannerPosition());
        assertTrue(config.isCollapsibleBannerEnabled());
    }

    @Test
    public void testRemoteConfigMapper() {
        Map<String, Object> remoteMap = new HashMap<>();
        remoteMap.put("ads_enabled", true);
        remoteMap.put("rewarded_interstitial_enabled", true);
        remoteMap.put("admob_rewarded_interstitial_id", "ca-app-pub-test/123");
        remoteMap.put("collapsible_banner_position", "bottom");

        SmartAdsConfig.Builder builder = new SmartAdsConfig.Builder();
        SmartAdsRemoteConfigMapper.applyRemoteConfig(builder, remoteMap);
        SmartAdsConfig config = builder.build();

        assertTrue(config.isAdsEnabled());
        assertTrue(config.isRewardedInterstitialEnabled());
        assertEquals("ca-app-pub-test/123", config.getAdMobRewardedInterstitialId());
        assertEquals("bottom", config.getCollapsibleBannerPosition());
    }

    @Test
    public void testConfigToBuilderPreservesFields() {
        SmartAdsConfig original = new SmartAdsConfig.Builder()
                .setRewardedInterstitialEnabled(true)
                .setAdMobRewardedInterstitialId("ri_123")
                .setCollapsibleBannerPosition("top")
                .build();

        SmartAdsConfig cloned = original.toBuilder().build();

        assertEquals(original.isRewardedInterstitialEnabled(), cloned.isRewardedInterstitialEnabled());
        assertEquals(original.getAdMobRewardedInterstitialId(), cloned.getAdMobRewardedInterstitialId());
        assertEquals(original.getCollapsibleBannerPosition(), cloned.getCollapsibleBannerPosition());
    }
}