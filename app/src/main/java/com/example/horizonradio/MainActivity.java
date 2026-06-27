package com.example.horizonradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String DRIVE_APP_URL =
            "https://drive.google.com/file/d/153ZOQxQWB0OzEuKlaf5Cjr_B9f-R_Kf5/view?usp=drivesdk";
    private static final String LOCAL_APP_URL = "file:///android_asset/www/index.html";

    private TextView status;
    private WebView webView;

    private final BroadcastReceiver telemetryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("error")) {
                status.setText("Telemetry error: " + intent.getStringExtra("error"));
                return;
            }

            status.setText(String.format(
                    Locale.US,
                    "Speed %.0f mph • RPM %.0f • Gear %d",
                    intent.getFloatExtra(TelemetryService.EXTRA_SPEED, 0),
                    intent.getFloatExtra(TelemetryService.EXTRA_RPM, 0),
                    intent.getIntExtra(TelemetryService.EXTRA_GEAR, 0)));
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(16, 24, 32));
        setContentView(root);

        status = new TextView(this);
        status.setText("Ready — load the Drive app or start Forza telemetry.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setPadding(24, 24, 24, 14);
        root.addView(status);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(16, 0, 16, 16);
        root.addView(toolbar);

        Button openDrive = toolbarButton("Open Drive App");
        openDrive.setOnClickListener(view -> webView.loadUrl(DRIVE_APP_URL));
        toolbar.addView(openDrive);

        Button telemetry = toolbarButton("Start Telemetry");
        telemetry.setOnClickListener(view -> startTelemetry());
        toolbar.addView(telemetry);

        Button spotify = toolbarButton("Spotify");
        spotify.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"))));
        toolbar.addView(spotify);

        Button help = toolbarButton("Help");
        help.setOnClickListener(view -> showHelp());
        toolbar.addView(help);

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        root.addView(webView);
        webView.loadUrl(LOCAL_APP_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(TelemetryService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(telemetryReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(telemetryReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(telemetryReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void startTelemetry() {
        Intent intent = new Intent(this, TelemetryService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        status.setText("Listening for Forza UDP telemetry on port 5300…");
    }

    private Button toolbarButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return button;
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("How this Android app uses your Drive file")
                .setMessage("The app includes a WebView shell for the Google Drive file you provided. "
                        + "If that file is a website/exported HTML app, make the file public or copy its "
                        + "contents into app/src/main/assets/www/ for fully offline packaging.\n\n"
                        + "For Forza, enable Data Out and send UDP telemetry to this Android device on port 5300. "
                        + "Spotify playback still runs through Spotify/Spotify Connect; Android cannot patch "
                        + "the PC game's internal radio bus by itself.")
                .setPositiveButton("OK", null)
                .show();
    }
}
