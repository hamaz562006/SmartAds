package com.partharoypc.smartads.ui.crash;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.partharoypc.smartads.R;

/**
 * Fallback Activity displayed when an uncaught crash occurs.
 */
public class SmartAdsCrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smartads_activity_crash);

        Button btnClose = findViewById(R.id.smartads_crash_btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> closeApp());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeApp();
    }

    private void closeApp() {
        finishAffinity();
        System.exit(0);
    }
}
