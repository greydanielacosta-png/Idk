package com.example.horizonradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class TelemetryService extends Service {
    public static final String ACTION_UPDATE = "com.example.horizonradio.UPDATE";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_RPM = "rpm";
    public static final String EXTRA_GEAR = "gear";

    private static final int NOTIFICATION_ID = 7;
    private static final int TELEMETRY_PORT = 5300;

    private volatile boolean running;
    private Thread worker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification("Waiting for Forza UDP telemetry"));
        startTelemetryListener();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
        super.onDestroy();
    }

    private void startTelemetryListener() {
        if (running) {
            return;
        }

        running = true;
        worker = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(TELEMETRY_PORT)) {
                socket.setSoTimeout(1500);
                byte[] buffer = new byte[1500];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        Telemetry telemetry = parse(packet.getData(), packet.getLength());
                        publishTelemetry(telemetry);
                    } catch (SocketTimeoutException ignored) {
                        // Timeout lets the loop observe the running flag.
                    }
                }
            } catch (Exception exception) {
                sendBroadcast(new Intent(ACTION_UPDATE).putExtra("error", exception.getMessage()));
            }
        }, "forza-telemetry-listener");
        worker.start();
    }

    private void publishTelemetry(Telemetry telemetry) {
        Intent update = new Intent(ACTION_UPDATE);
        update.putExtra(EXTRA_SPEED, telemetry.speedMph);
        update.putExtra(EXTRA_RPM, telemetry.rpm);
        update.putExtra(EXTRA_GEAR, telemetry.gear);
        sendBroadcast(update);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification(String.format(
                Locale.US,
                "%.0f mph • %.0f rpm • gear %d",
                telemetry.speedMph,
                telemetry.rpm,
                telemetry.gear)));
    }

    private Telemetry parse(byte[] data, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);
        float rpm = length > 16 ? buffer.getFloat(16) : 0;
        int gear = length > 319 ? Byte.toUnsignedInt(data[319]) : 0;
        float metersPerSecond = length > 256 ? buffer.getFloat(244) : 0;
        return new Telemetry(metersPerSecond * 2.236936f, rpm, gear);
    }

    private Notification notification(String text) {
        String channelId = "telemetry";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Telemetry listener",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, channelId)
                .setContentTitle("Horizon Telemetry Radio")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .build();
    }

    private static class Telemetry {
        final float speedMph;
        final float rpm;
        final int gear;

        Telemetry(float speedMph, float rpm, int gear) {
            this.speedMph = speedMph;
            this.rpm = rpm;
            this.gear = gear;
        }
    }
}
