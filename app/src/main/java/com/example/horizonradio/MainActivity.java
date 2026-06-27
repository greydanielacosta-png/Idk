package com.example.horizonradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String LOCAL_HELP_URL = "file:///android_asset/www/index.html";
    private static final String PREFS = "fh6-radio";
    private static final String KEY_PC_IP = "pc-ip";

    private SharedPreferences preferences;
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
                    "Forza telemetry: %.0f mph • %.0f rpm • gear %d",
                    intent.getFloatExtra(TelemetryService.EXTRA_SPEED, 0),
                    intent.getFloatExtra(TelemetryService.EXTRA_RPM, 0),
                    intent.getIntExtra(TelemetryService.EXTRA_GEAR, 0)));
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12, 18, 29));
        setContentView(root);

        status = new TextView(this);
        status.setText("FH6 Spotify Radio Companion — start with Setup, Dashboard, or Spotify.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(22, 22, 22, 12);
        root.addView(status);

        LinearLayout firstRow = toolbarRow();
        root.addView(firstRow);
        firstRow.addView(toolbarButton("Setup", view -> webView.loadUrl(LOCAL_HELP_URL)));
        firstRow.addView(toolbarButton("Dashboard", view -> openDashboardPrompt()));
        firstRow.addView(toolbarButton("Spotify", view -> openSpotify()));

        LinearLayout secondRow = toolbarRow();
        root.addView(secondRow);
        secondRow.addView(toolbarButton("Telemetry", view -> startTelemetry()));
        secondRow.addView(toolbarButton("FH6 Settings", view -> showForzaSettings()));
        secondRow.addView(toolbarButton("Troubleshoot", view -> showTroubleshooting()));

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
        webView.loadUrl(LOCAL_HELP_URL);
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

    private LinearLayout toolbarRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(14, 0, 14, 8);
        return row;
    }

    private Button toolbarButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return button;
    }

    private void openDashboardPrompt() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("PC IP address, e.g. 192.168.1.25");
        input.setText(preferences.getString(KEY_PC_IP, ""));

        new AlertDialog.Builder(this)
                .setTitle("Open FH6 Radio dashboard")
                .setMessage("Enter the Windows PC IP address running Forza Horizon 6 and the Spotify Radio mod. The dashboard runs on port 8103.")
                .setView(input)
                .setPositiveButton("Open", (dialog, which) -> {
                    String pcIp = input.getText().toString().trim();
                    if (pcIp.isEmpty()) {
                        status.setText("Enter your PC IP address before opening the dashboard.");
                        return;
                    }
                    preferences.edit().putString(KEY_PC_IP, pcIp).apply();
                    String dashboardUrl = "http://" + pcIp + ":8103";
                    status.setText("Opening FH6 Radio dashboard: " + dashboardUrl);
                    webView.loadUrl(dashboardUrl);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSpotify() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:")));
            status.setText("Spotify opened — select FH6 Radio from Spotify Connect devices.");
        } catch (Exception exception) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")));
            status.setText("Spotify app was not found, opened Spotify Web instead.");
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

    private void showForzaSettings() {
        new AlertDialog.Builder(this)
                .setTitle("FH6 in-game settings")
                .setMessage("For the Spotify Radio PC mod:\n\n"
                        + "• Radio DJ: Off\n"
                        + "• Streamer Mode: On\n"
                        + "• Select Spotify Radio / Streamer Mode / R10 in the in-game radio.\n\n"
                        + "For this Android telemetry display, send FH6 Data Out to this phone on UDP port 5300.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTroubleshooting() {
        new AlertDialog.Builder(this)
                .setTitle("If Spotify cannot see FH6 Radio")
                .setMessage("Check these first:\n\n"
                        + "• Spotify Premium is normally required.\n"
                        + "• Phone and PC must be on the same local network.\n"
                        + "• VPN, firewall, private DNS, or router isolation can block Spotify Connect discovery.\n"
                        + "• version.dll must be next to forzahorizon6.exe.\n"
                        + "• Open the Dashboard button in this app and confirm the PC mod is running at port 8103.\n\n"
                        + "This APK cannot replace the Windows PC mod; it helps control Spotify and monitor setup from Android.")
                .setPositiveButton("OK", null)
                .show();
    }
}
