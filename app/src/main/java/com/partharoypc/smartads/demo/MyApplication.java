package com.partharoypc.smartads.demo;

import android.app.Application;

import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;

import java.io.File;

public class MyApplication extends Application {
        @Override
        public void onCreate() {
                super.onCreate();

                // Fix potential Chromium WebView Cache warning/error by ensuring the wasm directory exists
                try {
                        File webViewWasmDir = new File(getCacheDir(), "WebView/Default/HTTP Cache/Code Cache/wasm");
                        if (!webViewWasmDir.exists()) {
                                webViewWasmDir.mkdirs();
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

                SmartAdsConfig config = new SmartAdsConfig.Builder()
                                .setAdsEnabled(true) // Global Master Switch
                                .setBannerEnabled(true) // Granular Toggle
                                .setInterstitialEnabled(true) // Granular Toggle
                                .setRewardedEnabled(true) // Granular Toggle
                                .setRewardedInterstitialEnabled(true) // Granular Toggle
                                .setAppOpenEnabled(true) // Granular Toggle
                                .setNativeEnabled(true) // Granular Toggle
                                .setTestModeEnabled(true)
                                .setAdMobBannerId("ca-app-pub-3940256099942544/6300978111")
                                .setAdMobInterstitialId("ca-app-pub-3940256099942544/1033173712")
                                .setAdMobRewardedId("ca-app-pub-3940256099942544/5224354917")
                                .setAdMobRewardedInterstitialId("ca-app-pub-3940256099942544/5354046379")
                                .setAdMobNativeId("ca-app-pub-3940256099942544/2247696110")
                                .setAdMobAppOpenId("ca-app-pub-3940256099942544/9257395921")
                                .setCollapsibleBannerEnabled(false)
                                .setUseUmpConsent(true)
                                .setFrequencyCapSeconds(15)
                                .setLoadingDialogText("Loading awesome ad...")
                                .setLoadingDialogColor(android.graphics.Color.DKGRAY, android.graphics.Color.YELLOW)

                                // House Ads
                                .addHouseAd(new com.partharoypc.smartads.house.HouseAd.Builder()
                                                .setId("demo_house_ad_1")
                                                .setTitle("Check out Know Bangladesh!")
                                                .setDescription("Learn everything about Bangladesh in one app.")
                                                .setCtaText("Download")
                                                .setIconResId(R.drawable.ic_launcher_background)
                                                .setImageResId(R.drawable.gradient_header)
                                                .setRating(4.8f)
                                                .setClickUrl("https://play.google.com/store/apps/details?id=com.partharoy.knowbangladesh")
                                                .build())
                                .addHouseAd(new com.partharoypc.smartads.house.HouseAd.Builder()
                                                .setId("demo_house_ad_2")
                                                .setTitle("Smart Ads Pro")
                                                .setDescription("The best way to monetize your Android apps.")
                                                .setCtaText("Get It Now")
                                                .setIconResId(R.drawable.ic_launcher_foreground)
                                                .setImageResId(android.R.drawable.ic_dialog_map)
                                                .setRating(5.0f)
                                                .setClickUrl("https://github.com/partharoypc")
                                                .build())
                                .addHouseAd(new com.partharoypc.smartads.house.HouseAd.Builder()
                                                .setId("demo_house_ad_3")
                                                .setTitle("Another Cool App")
                                                .setDescription("This is a third house ad to test rotation.")
                                                .setCtaText("Try Free")
                                                .setIconResId(android.R.drawable.ic_menu_camera)
                                                .setImageResId(android.R.drawable.ic_menu_gallery)
                                                .setRating(4.5f)
                                                .setClickUrl("https://google.com")
                                                .build())
                                .setHouseAdsEnabled(true)
                                .build();

                SmartAds.initialize(this, config);
        }
}
