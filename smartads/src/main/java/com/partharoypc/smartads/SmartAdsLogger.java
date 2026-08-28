package com.partharoypc.smartads;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class SmartAdsLogger {
    private static final String TAG = "SmartAds";

    public interface LogListener {
        void onLog(String message);
    }

    private static volatile LogListener logListener;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SmartAdsLogger() {
        // Utility class
    }

    public static void setLogListener(LogListener listener) {
        logListener = listener;
    }

    public static void d(String message) {
        try {
            if (SmartAds.isInitialized() && SmartAds.getInstance().getConfig() != null
                    && SmartAds.getInstance().getConfig().isLoggingEnabled()) {
                Log.d(TAG, message);
            }
        } catch (Exception ignored) {
        }
        dispatchLog(message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
        dispatchLog(message);
    }

    public static void e(String message, Throwable tr) {
        Log.e(TAG, message, tr);
        dispatchLog(message + (tr != null ? " (" + tr.getMessage() + ")" : ""));
    }

    private static void dispatchLog(String message) {
        LogListener listener = logListener;
        if (listener != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                listener.onLog(message);
            } else {
                mainHandler.post(() -> {
                    LogListener l = logListener;
                    if (l != null) {
                        l.onLog(message);
                    }
                });
            }
        }
    }
}
