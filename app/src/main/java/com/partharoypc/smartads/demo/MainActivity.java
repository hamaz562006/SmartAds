package com.partharoypc.smartads.demo;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.partharoypc.smartads.NativeAdSize;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.listeners.BannerAdListener;
import com.partharoypc.smartads.listeners.InterstitialAdListener;
import com.partharoypc.smartads.listeners.NativeAdListener;
import com.partharoypc.smartads.listeners.RewardedAdListener;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextView textSdkStatus;
    private com.google.android.material.switchmaterial.SwitchMaterial switchEnableAds;
    private com.google.android.material.switchmaterial.SwitchMaterial switchForceHouseAds;

    // Banner
    private FrameLayout bannerContainer;
    private Button btnLoadBanner, btnLoadCollapsible;

    // Interstitial
    private TextView statusInterstitial;
    private Button btnLoadInterstitial, btnShowInterstitial;

    // Rewarded
    private TextView statusRewarded;
    private Button btnLoadRewarded, btnShowRewarded;

    // Rewarded Interstitial
    private TextView statusRewardedInterstitial;
    private Button btnLoadRewardedInterstitial, btnShowRewardedInterstitial;

    // App Open
    private TextView statusAppOpen;
    private Button btnShowAppOpen, btnTestSplash;
    private com.google.android.material.switchmaterial.SwitchMaterial switchAppOpenActivity;

    // Native
    private RadioGroup radioGroupNativeSize;
    private Button btnLoadNative;
    private FrameLayout nativeContainer;

    // Interstitial interval
    private Button btnShowInterstitialInterval;
    private int clickCount = 0;

    // Debug Utilities
    private Button btnAdInspector, btnPrivacyOptions, btnShutdownSdk, btnPreviewLoadingDialog, btnPreloadAds;
    private com.google.android.material.textfield.TextInputEditText inputLoadingText;
    private Button btnColorPurple, btnColorOrange;

    // Logger
    private TextView textLogger;
    private Button btnClearLog, btnCopyLog;
    private ScrollView logScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        updateSdkStatus();

        // Connect SDK Logger to Demo's Live Event Log
        SmartAdsLogger.setLogListener(msg -> {
            log(msg);
            updateAdStatuses();
        });

        // Setup Analytics Listener for Demo
        SmartAds.getInstance()
                .setAnalyticsListener((adUnitId, adFormat, adNetwork, valueMicros, currencyCode, precision, extras) -> {
                    double revenue = valueMicros / 1000000.0;
                    log("💰 Paid: " + revenue + " " + currencyCode + " [" + adNetwork + "] (" + adFormat + ")");
                });

        log("App Started. Ready to test ads.");
    }

    private void initViews() {
        // Implementation Header
        textSdkStatus = findViewById(R.id.text_sdk_status);
        switchEnableAds = findViewById(R.id.switch_enable_ads);
        switchForceHouseAds = findViewById(R.id.switch_force_house_ads);

        // Banner
        bannerContainer = findViewById(R.id.banner_container);
        btnLoadBanner = findViewById(R.id.btn_load_banner);
        btnLoadCollapsible = findViewById(R.id.btn_load_collapsible);

        // Interstitial
        statusInterstitial = findViewById(R.id.status_interstitial);
        btnLoadInterstitial = findViewById(R.id.btn_load_interstitial);
        btnShowInterstitial = findViewById(R.id.btn_show_interstitial);
        btnShowInterstitialInterval = findViewById(R.id.btn_show_interstitial_interval);

        // Rewarded
        statusRewarded = findViewById(R.id.status_rewarded);
        btnLoadRewarded = findViewById(R.id.btn_load_rewarded);
        btnShowRewarded = findViewById(R.id.btn_show_rewarded);

        // Rewarded Interstitial
        statusRewardedInterstitial = findViewById(R.id.status_rewarded_interstitial);
        btnLoadRewardedInterstitial = findViewById(R.id.btn_load_rewarded_interstitial);
        btnShowRewardedInterstitial = findViewById(R.id.btn_show_rewarded_interstitial);

        // App Open
        statusAppOpen = findViewById(R.id.status_app_open);
        btnShowAppOpen = findViewById(R.id.btn_show_app_open);
        btnTestSplash = findViewById(R.id.btn_test_splash);
        switchAppOpenActivity = findViewById(R.id.switch_app_open_activity);

        // Native
        radioGroupNativeSize = findViewById(R.id.radio_group_native_size);
        btnLoadNative = findViewById(R.id.btn_load_native);
        nativeContainer = findViewById(R.id.native_container);

        // Debug
        btnAdInspector = findViewById(R.id.btn_ad_inspector);
        btnPrivacyOptions = findViewById(R.id.btn_privacy_options);
        btnShutdownSdk = findViewById(R.id.btn_shutdown_sdk);
        btnPreviewLoadingDialog = findViewById(R.id.btn_preview_loading_dialog);
        btnPreloadAds = findViewById(R.id.btn_preload_ads);
        inputLoadingText = findViewById(R.id.input_loading_text);
        btnColorPurple = findViewById(R.id.btn_color_purple);
        btnColorOrange = findViewById(R.id.btn_color_orange);

        // Logs
        textLogger = findViewById(R.id.text_logger);
        btnClearLog = findViewById(R.id.btn_clear_log);
        btnCopyLog = findViewById(R.id.btn_copy_log);
        logScrollView = (ScrollView) textLogger.getParent();
    }

    private void setupListeners() {
        // --- General Settings Switches ---

        switchEnableAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleAds(isChecked);
            }
        });

        switchForceHouseAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleHouseAds(isChecked);
            }
        });

        // Banner
        btnLoadBanner.setOnClickListener(v -> loadBanner(false));
        btnLoadCollapsible.setOnClickListener(v -> loadBanner(true));

        // Interstitial
        btnLoadInterstitial.setOnClickListener(v -> loadInterstitial());
        btnShowInterstitial.setOnClickListener(v -> showInterstitial());
        btnShowInterstitialInterval.setOnClickListener(v -> showInterstitialInterval());

        // Rewarded
        btnLoadRewarded.setOnClickListener(v -> loadRewarded());
        btnShowRewarded.setOnClickListener(v -> showRewarded());

        // Rewarded Interstitial
        btnLoadRewardedInterstitial.setOnClickListener(v -> loadRewardedInterstitial());
        btnShowRewardedInterstitial.setOnClickListener(v -> showRewardedInterstitial());

        // App Open
        btnShowAppOpen.setOnClickListener(v -> {
            log("Attempting to show App Open Ad...");
            SmartAds.getInstance().showAppOpenAd(this);
        });

        btnTestSplash.setOnClickListener(v -> testSplashFlow());

        switchAppOpenActivity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SmartAds.getInstance().enableAppOpenForActivity(MainActivity.class);
                log("🟢 App Open Ads ENABLED for MainActivity");
                showSnackbar("App Open Enabled for MainActivity");
            } else {
                SmartAds.getInstance().disableAppOpenForActivity(MainActivity.class);
                log("🔴 App Open Ads DISABLED for MainActivity");
                showSnackbar("App Open Disabled for MainActivity");
            }
        });

        // Native
        btnLoadNative.setOnClickListener(v -> loadNativeAd());

        // Log
        btnClearLog.setOnClickListener(v -> textLogger.setText("> Logs cleared...\n"));
        btnCopyLog.setOnClickListener(v -> {
            String logs = textLogger.getText().toString();
            if (!logs.isEmpty()) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Live Event Log", logs);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showSnackbar("Logs copied to clipboard");
                }
            }
        });

        // Debug Utilities
        btnAdInspector.setOnClickListener(v -> {
            log("Opening Ad Inspector...");
            SmartAds.getInstance().launchAdInspector(this);
        });

        btnPrivacyOptions.setOnClickListener(v -> {
            if (SmartAds.getInstance().isPrivacyOptionsRequired()) {
                log("Opening Privacy Options Form...");
                SmartAds.getInstance().showPrivacyOptionsForm(this);
            } else {
                log("Privacy Options Form not required at this time.");
                showSnackbar("Privacy Options not required");
            }
        });

        btnShutdownSdk.setOnClickListener(v -> {
            log("🛑 Shutting down SmartAds SDK...");
            try {
                SmartAds.getInstance().shutdown();
                log("SDK Shutdown complete. Resources cleared.");
                updateSdkStatus();
                showSnackbar("SmartAds Shutdown");

                // Disable all ad buttons
                disableAllAdButtons();
            } catch (Exception e) {
                log("Shutdown Error: " + e.getMessage());
            }
        });

        btnColorPurple.setOnClickListener(v -> {
            log("🎨 Setting Dialog Color to Purple Theme");
            SmartAds.getInstance().updateConfig(SmartAds.getInstance().getConfig().toBuilder()
                    .setLoadingDialogColor(0xFF581C87, 0xFFFAF5FF) // bg: purple-900, text: purple-50
                    .setLoadingDialogProgressColor(0xFFA855F7) // purple-500
                    .build());
            showSnackbar("Dialog Color: Purple");
        });

        btnColorOrange.setOnClickListener(v -> {
            log("🎨 Setting Dialog Color to Orange Theme");
            SmartAds.getInstance().updateConfig(SmartAds.getInstance().getConfig().toBuilder()
                    .setLoadingDialogColor(0xFF7C2D12, 0xFFFFF7ED) // bg: orange-900, text: orange-50
                    .setLoadingDialogProgressColor(0xFFF97316) // orange-500
                    .build());
            showSnackbar("Dialog Color: Orange");
        });

        btnPreviewLoadingDialog.setOnClickListener(v -> {
            String customText = inputLoadingText.getText().toString().trim();
            if (!customText.isEmpty()) {
                SmartAds.getInstance().updateConfig(SmartAds.getInstance().getConfig().toBuilder()
                        .setLoadingDialogText(customText)
                        .build());
            }

            log("Previewing Loading Dialog for 3 seconds...");
            com.partharoypc.smartads.ui.LoadingAdDialog dialog = new com.partharoypc.smartads.ui.LoadingAdDialog(this);
            dialog.setSubHeadline(SmartAds.getInstance().getConfig().getDialogSubText());
            dialog.show(SmartAds.getInstance().getConfig().getDialogText(),
                    SmartAds.getInstance().getConfig().getDialogBackgroundColor(),
                    SmartAds.getInstance().getConfig().getDialogTextColor());

            // Dismiss after 3 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                dialog.dismiss();
                log("Loading Dialog Dismissed (Preview ended)");
            }, 3000);
        });

        btnPreloadAds.setOnClickListener(v -> {
            log("🚀 Preloading ALL Ads...");
            SmartAds.getInstance().preloadAds(this);
            showSnackbar("Started Preloading Ads");
        });
    }

    private void disableAllAdButtons() {
        btnLoadBanner.setEnabled(false);
        btnLoadCollapsible.setEnabled(false);
        btnLoadInterstitial.setEnabled(false);
        btnShowInterstitial.setEnabled(false);
        btnShowInterstitialInterval.setEnabled(false);
        btnLoadRewarded.setEnabled(false);
        btnShowRewarded.setEnabled(false);
        btnLoadRewardedInterstitial.setEnabled(false);
        btnShowRewardedInterstitial.setEnabled(false);
        btnShowAppOpen.setEnabled(false);
        btnTestSplash.setEnabled(false);
        btnLoadNative.setEnabled(false);

        btnAdInspector.setEnabled(false);
        btnPrivacyOptions.setEnabled(false);
        btnShutdownSdk.setEnabled(false);
        btnPreviewLoadingDialog.setEnabled(false);

        textSdkStatus.setText(R.string.sdk_shutdown_message);
        textSdkStatus.setTextColor(ContextCompat.getColor(this, R.color.red_error));
    }

    private void updateSdkStatus() {
        if (!SmartAds.isInitialized()) {
            textSdkStatus.setText(R.string.sdk_not_initialized);
            textSdkStatus.setTextColor(ContextCompat.getColor(this, R.color.red_error));
            return;
        }
        com.partharoypc.smartads.SmartAdsConfig config = SmartAds.getInstance().getConfig();
        boolean isTestMode = config.isTestMode();
        boolean areAdsEnabled = SmartAds.getInstance().areAdsEnabled();
        String version = SmartAds.getVersion();

        String mode = isTestMode ? getString(R.string.mode_test) : getString(R.string.mode_prod);
        textSdkStatus.setText(getString(R.string.sdk_status_format, version, mode));

        if (!areAdsEnabled) {
            textSdkStatus.append("\n⛔ Ads Disabled");
            textSdkStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            textSdkStatus.setTextColor(0xFFF0F0F0);
        }

        // Sync Switches without triggering listeners loops (simple check)
        if (switchEnableAds.isChecked() != areAdsEnabled) {
            switchEnableAds.setChecked(areAdsEnabled);
        }
    }

    private void toggleHouseAds(boolean forceHouseAds) {
        if (!SmartAds.isInitialized())
            return;

        SmartAdsConfig current = SmartAds.getInstance().getConfig();
        SmartAdsConfig.Builder builder = current.toBuilder();

        if (forceHouseAds) {
            log("🔧 Forcing House Ads (Switching to Invalid AdMob IDs)...");
            builder.setAdMobBannerId("invalid_id")
                    .setAdMobInterstitialId("invalid_id")
                    .setAdMobRewardedId("invalid_id")
                    .setAdMobRewardedInterstitialId("invalid_id")
                    .setAdMobNativeId("invalid_id")
                    .setAdMobAppOpenId("invalid_id")
                    .setHouseAdsEnabled(true); // Ensure house ads are ON
        } else {
            log("✅ Restoring AdMob Test IDs...");
            // Restore Test IDs (as seen in MyApplication)
            builder.setAdMobBannerId("ca-app-pub-3940256099942544/9214589741")
                    .setAdMobInterstitialId("ca-app-pub-3940256099942544/1033173712")
                    .setAdMobRewardedId("ca-app-pub-3940256099942544/5224354917")
                    .setAdMobRewardedInterstitialId("ca-app-pub-3940256099942544/5354046379")
                    .setAdMobNativeId("ca-app-pub-3940256099942544/2247696110")
                    .setAdMobAppOpenId("ca-app-pub-3940256099942544/9257395921");
        }

        SmartAds.getInstance().updateConfig(builder.build());
        showSnackbar("Force House Ads: " + (forceHouseAds ? "ON" : "OFF"));
    }

    private void toggleAds(boolean enabled) {
        SmartAds.getInstance().setAdsEnabled(enabled);
        updateSdkStatus();
        showSnackbar("Ads " + (enabled ? "Enabled" : "Disabled"));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            updateAdStatuses();
        }
    }

    private void updateAdStatuses() {
        if (!SmartAds.isInitialized())
            return;

        // App Open Ad Status Update
        if (SmartAds.getInstance().isAppOpenAdAvailable()) {
            setStatus(statusAppOpen, "Available", R.color.green_success);
            btnShowAppOpen.setEnabled(true);
        } else {
            setStatus(statusAppOpen, SmartAds.getInstance().getAppOpenAdStatus().name(), R.color.text_secondary);
        }

        // Interstitial Status
        if (SmartAds.getInstance().isInterstitialAdAvailable()) {
            setStatus(statusInterstitial, "READY", R.color.green_success);
            btnShowInterstitial.setEnabled(true);
        } else {
            // Show actual status (LOADING, FAILED, IDLE, etc.)
            String status = SmartAds.getInstance().getInterstitialAdStatus().name();
            setStatus(statusInterstitial, status, R.color.text_secondary);
            btnShowInterstitial.setEnabled(false);
        }

        // Rewarded Status
        if (SmartAds.getInstance().isRewardedAdAvailable()) {
            setStatus(statusRewarded, "READY", R.color.green_success);
            btnShowRewarded.setEnabled(true);
        } else {
            String status = SmartAds.getInstance().getRewardedAdStatus().name();
            setStatus(statusRewarded, status, R.color.text_secondary);
            btnShowRewarded.setEnabled(false);
        }

        // Rewarded Interstitial Status
        if (SmartAds.getInstance().isRewardedInterstitialAdAvailable()) {
            setStatus(statusRewardedInterstitial, "READY", R.color.green_success);
            btnShowRewardedInterstitial.setEnabled(true);
        } else {
            String status = SmartAds.getInstance().getRewardedInterstitialAdStatus().name();
            setStatus(statusRewardedInterstitial, status, R.color.text_secondary);
            btnShowRewardedInterstitial.setEnabled(false);
        }
    }

    // Helper to set status with color
    private void setStatus(TextView view, String text, int colorResId) {
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, colorResId));
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.surface_card))
                .setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                .show();
    }

    // ================= BANNER =================
    private void loadBanner(boolean collapsible) {
        log("Loading Banner (Collapsible: " + collapsible + ")...");

        // Update config to enable/disable collapsible banner for this request
        com.partharoypc.smartads.SmartAdsConfig current = SmartAds.getInstance().getConfig();
        SmartAds.getInstance().updateConfig(current.toBuilder()
                .setCollapsibleBannerEnabled(collapsible)
                .build());

        // Ensure container is visible
        bannerContainer.setVisibility(View.VISIBLE);

        SmartAds.getInstance().showBannerAd(this, bannerContainer, new BannerAdListener() {
            @Override
            public void onAdLoaded(View adView) {
                log("Banner Loaded");
            }

            @Override
            public void onAdFailed(String errorMessage) {
                log("Banner Failed: " + errorMessage);
                showSnackbar("Banner Failed to load");
            }
        });
    }

    // ================= INTERSTITIAL =================
    private void loadInterstitial() {
        log("Loading Interstitial...");
        setStatus(statusInterstitial, "Loading...", R.color.text_primary);
        btnShowInterstitial.setEnabled(false); // Disable until loaded

        SmartAds.getInstance().loadInterstitialAd(this);

        // Check availability after a short delay to simulate "polling"
        // In a real scenario, you might have a listener or check in onResume
        statusInterstitial.postDelayed(this::updateAdStatuses, 2000);
    }

    private void showInterstitial() {
        if (SmartAds.getInstance().isInterstitialAdAvailable()) {
            SmartAds.getInstance().showInterstitialAd(this, new InterstitialAdListener() {
                @Override
                public void onAdDismissed() {
                    log("Interstitial Dismissed");
                    setStatus(statusInterstitial, "Dismissed", R.color.text_secondary);
                    btnShowInterstitial.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(String errorMessage) {
                    log("Interstitial Failed to Show: " + errorMessage);
                    setStatus(statusInterstitial, "Failed", R.color.red_error);
                }
            });
        } else {
            log("Interstitial not ready yet.");
            showSnackbar("Interstitial ad not ready");
        }
    }

    private void showInterstitialInterval() {
        clickCount++;
        log("Interval action triggered (Click #" + clickCount + " - interval target: 3)...");
        SmartAds.getInstance().showInterstitialAdWithInterval(this, 3, new InterstitialAdListener() {
            @Override
            public void onAdDismissed() {
                log("Interval Interstitial Dismissed");
                updateAdStatuses();
            }

            @Override
            public void onAdFailedToShow(String errorMessage) {
                log("Interval Interstitial Failed to Show: " + errorMessage);
                updateAdStatuses();
            }
        });
    }

    // ================= REWARDED =================
    private void loadRewarded() {
        log("Loading Rewarded Ad...");
        setStatus(statusRewarded, "Loading...", R.color.text_primary);
        btnShowRewarded.setEnabled(false);

        SmartAds.getInstance().loadRewardedAd(this);

        statusRewarded.postDelayed(this::updateAdStatuses, 2000);
    }

    private void showRewarded() {
        if (SmartAds.getInstance().isRewardedAdAvailable()) {
            SmartAds.getInstance().showRewardedAd(this, new RewardedAdListener() {
                @Override
                public void onUserEarnedReward() {
                    log("User Earned Reward!");
                    showSnackbar("🎁 Reward Earned!");
                }

                @Override
                public void onAdDismissed() {
                    log("Rewarded Ad Dismissed");
                    setStatus(statusRewarded, "Dismissed", R.color.text_secondary);
                    btnShowRewarded.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(String errorMessage) {
                    log("Rewarded Failed: " + errorMessage);
                    setStatus(statusRewarded, "Failed", R.color.red_error);
                }
            });
        } else {
            log("Rewarded Ad not ready yet.");
            showSnackbar("Rewarded ad not ready");
        }
    }

    // ================= REWARDED INTERSTITIAL =================
    private void loadRewardedInterstitial() {
        log("Loading Rewarded Interstitial...");
        setStatus(statusRewardedInterstitial, "Loading...", R.color.text_primary);
        btnShowRewardedInterstitial.setEnabled(false);

        SmartAds.getInstance().loadRewardedInterstitialAd(this);

        statusRewardedInterstitial.postDelayed(this::updateAdStatuses, 2000);
    }

    private void showRewardedInterstitial() {
        if (SmartAds.getInstance().isRewardedInterstitialAdAvailable()) {
            SmartAds.getInstance().showRewardedInterstitialAd(this, new com.partharoypc.smartads.listeners.RewardedInterstitialAdListener() {
                @Override
                public void onUserEarnedReward() {
                    log("🎁 User Earned Rewarded Interstitial Reward!");
                    showSnackbar("🎁 Rewarded Interstitial Reward Earned!");
                }

                @Override
                public void onAdDismissed() {
                    log("Rewarded Interstitial Dismissed");
                    setStatus(statusRewardedInterstitial, "Dismissed", R.color.text_secondary);
                    btnShowRewardedInterstitial.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(String errorMessage) {
                    log("Rewarded Interstitial Failed to Show: " + errorMessage);
                    setStatus(statusRewardedInterstitial, "Failed", R.color.red_error);
                }
            });
        } else {
            log("Rewarded Interstitial not ready yet.");
            showSnackbar("Rewarded Interstitial ad not ready");
        }
    }

    // ================= SPLASH HELPER SIMULATION =================
    private void testSplashFlow() {
        log("Testing Splash Flow (timeout 5s)...");
        showSnackbar("Starting Splash Flow Test...");
        com.partharoypc.smartads.utils.SplashAdHelper.loadAndShowSplashAd(
                this,
                com.partharoypc.smartads.utils.SplashAdHelper.SplashAdType.INTERSTITIAL,
                5000L,
                () -> {
                    log("✨ Splash Flow Complete! Moving to main app content.");
                    showSnackbar("Splash Flow Complete!");
                }
        );
    }

    // ================= NATIVE =================
    private void loadNativeAd() {
        int selectedId = radioGroupNativeSize.getCheckedRadioButtonId();

        log("Loading Native Ad...");

        // Clear previous
        SmartAds.getInstance().clearNativeIn(nativeContainer);

        if (selectedId == R.id.rb_custom) {
            // Load Custom Layout
            log("Loading Custom Native Layout...");
            SmartAds.getInstance().showNativeAd(this, nativeContainer, R.layout.layout_custom_native_ad,
                    new NativeAdListener() {
                        @Override
                        public void onAdLoaded(View nativeAdView) {
                            log("Custom Native Ad Loaded");
                        }

                        @Override
                        public void onAdFailed(String errorMessage) {
                            log("Custom Native Ad Failed: " + errorMessage);
                            showSnackbar("Native ad failed to load");
                        }
                    });
        } else {
            // Load Standard Template
            NativeAdSize size = NativeAdSize.MEDIUM;
            if (selectedId == R.id.rb_small)
                size = NativeAdSize.SMALL;
            else if (selectedId == R.id.rb_large)
                size = NativeAdSize.LARGE;

            log("Loading Standard Native Ad (" + size.name() + ")...");
            SmartAds.getInstance().showNativeAd(this, nativeContainer, size, new NativeAdListener() {

                @Override
                public void onAdLoaded(View nativeAdView) {
                    log("Native Ad Loaded");
                }

                @Override
                public void onAdFailed(String errorMessage) {
                    log("Native Ad Failed: " + errorMessage);
                    showSnackbar("Native ad failed to load");
                }
            });
        }
    }

    // ================= HELPER =================
    private void log(String message) {
        String time = DateFormat.format("HH:mm:ss", new Date()).toString();
        // Use green color in log view (handled by setup but we just append text here)
        String fullMsg = time + ": " + message + "\n";
        textLogger.append(fullMsg);

        // Auto scroll
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        Log.d("SmartAdsTest", message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (SmartAds.isInitialized()) {
            SmartAds.getInstance().destroyBannerIn(bannerContainer);
            SmartAds.getInstance().clearNativeIn(nativeContainer);
        }
    }
}
