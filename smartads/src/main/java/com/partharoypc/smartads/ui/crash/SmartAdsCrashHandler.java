package com.partharoypc.smartads.ui.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import androidx.annotation.NonNull;
import com.partharoypc.smartads.SmartAdsLogger;

/**
 * Custom UncaughtExceptionHandler that catches unexpected runtime crashes,
 * logs the details, displays SmartAdsCrashActivity, and cleanly terminates the process.
 */
public class SmartAdsCrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public SmartAdsCrashHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.context = context.getApplicationContext();
        this.defaultHandler = defaultHandler;
    }

    /**
     * Installs the SmartAdsCrashHandler as the default uncaught exception handler.
     *
     * @param context Application context.
     */
    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(
                new SmartAdsCrashHandler(context, Thread.getDefaultUncaughtExceptionHandler()));
        SmartAdsLogger.d("SmartAdsCrashHandler installed.");
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            SmartAdsLogger.e("FATAL UNCAUGHT EXCEPTION in thread [" + thread.getName() + "]: " + throwable.getMessage(), throwable);

            Intent intent = new Intent(context, SmartAdsCrashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            SmartAdsLogger.e("Failed to launch SmartAdsCrashActivity: " + e.getMessage());
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
                return;
            }
        }

        try {
            Process.killProcess(Process.myPid());
            System.exit(10);
        } catch (Exception ignored) {
        }
    }
}
