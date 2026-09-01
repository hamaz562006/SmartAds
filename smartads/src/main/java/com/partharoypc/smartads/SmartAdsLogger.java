package com.partharoypc.smartads;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class SmartAdsLogger {
    private static final String TAG = "SmartAds";

    public interface LogListener {
        void onLog(String message);
    }

    private static volatile boolean loggingOverride = false;
    private static volatile boolean hasOverride = false;
    private static volatile LogListener logListener;
    private static Handler mainHandler;

    static {
        try {
            if (Looper.getMainLooper() != null) {
                mainHandler = new Handler(Looper.getMainLooper());
            }
        } catch (Throwable ignored) {
            mainHandler = null;
        }
    }

    private SmartAdsLogger() {
        // Utility class
    }

    public static void setLoggingEnabled(boolean enabled) {
        loggingOverride = enabled;
        hasOverride = true;
    }

    public static boolean isLoggingEnabled() {
        if (hasOverride) {
            return loggingOverride;
        }
        try {
            if (SmartAds.isInitialized() && SmartAds.getInstance().getConfig() != null) {
                return SmartAds.getInstance().getConfig().isLoggingEnabled();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void setLogListener(LogListener listener) {
        logListener = listener;
    }

    public static void d(String message) {
        try {
            if (isLoggingEnabled()) {
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
            try {
                if (Looper.myLooper() == Looper.getMainLooper() || mainHandler == null) {
                    listener.onLog(message);
                } else {
                    mainHandler.post(() -> {
                        LogListener l = logListener;
                        if (l != null) {
                            l.onLog(message);
                        }
                    });
                }
            } catch (Throwable t) {
                listener.onLog(message);
            }
        }
    }
}
