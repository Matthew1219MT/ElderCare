package com.eldercare.eldercare.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.receiver.NotificationDismissReceiver;
import com.eldercare.eldercare.view.V_HomePage;

public class FallDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;
    private Handler handler = new Handler();
    private Runnable autoLaunchRunnable;
    private static final int NOTIFICATION_ID = 3;
    private static final float G = 9.81f;
    private static final float IMPACT_THRESHOLD = 1.0f * G;
    private static final float GYRO_THRESHOLD = 1.0f;
    private static final long WINDOW_MS = 500;
    private static final long COOLDOWN_MS = 5000;
    private long lastImpactTime = 0;
    private long lastFallTime = 0;

    private static FallDetectionService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        createNotificationChannel();

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Start foreground notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "fall_channel")
                .setContentTitle("Fall Detect Active")
                .setContentText("Eldercare will monitor for falls in the background")
                .setSmallIcon(R.drawable.warning)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.drawable.eldercare_icon));
        startForeground(2, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fall_channel",
                    "Fall Detection",
                    NotificationManager.IMPORTANCE_HIGH // Banner + sound/vibration
            );
            channel.setDescription("Fall detection alerts");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // keep running unless explicitly stopped
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        cancelAutoLaunch();
        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not binding to activity
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            float accelMag = (float) Math.sqrt(ax*ax + ay*ay + az*az);

            if (accelMag > IMPACT_THRESHOLD) {
                lastImpactTime = now;
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];
            float gyroMag = (float) Math.sqrt(gx*gx + gy*gy + gz*gz);

            // Check: gyro spike + recent impact
            if (gyroMag > GYRO_THRESHOLD && (now - lastImpactTime) < WINDOW_MS) {
                if ((now - lastFallTime) > COOLDOWN_MS) {
                    lastFallTime = now;
                    sendFallNotification();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendFallNotification() {
        cancelAutoLaunch();

        if (isAppInForeground()) {
            Intent dialogIntent = new Intent(this, V_HomePage.class);
            dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            dialogIntent.putExtra("SHOW_DIALOG", true);
            startActivity(dialogIntent);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent activityIntent = new Intent(this, V_HomePage.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityIntent.putExtra("SHOW_DIALOG", true);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(this, NotificationDismissReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "fall_channel")
                .setContentTitle("Fall Detected!")
                .setContentText("Please press on this notification to open Eldercare or dismiss if this is a false fall trigger.")
                .setSmallIcon(R.drawable.eldercare_notif_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.drawable.eldercare_icon))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(activityPendingIntent)
                .setDeleteIntent(dismissPendingIntent);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }
    private boolean isAppInForeground() {
        android.app.ActivityManager.RunningAppProcessInfo appProcessInfo =
                new android.app.ActivityManager.RunningAppProcessInfo();
        android.app.ActivityManager.getMyMemoryState(appProcessInfo);
        return appProcessInfo.importance ==
                android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    public static void cancelAutoLaunch() {
        if (instance != null && instance.autoLaunchRunnable != null) {
            instance.handler.removeCallbacks(instance.autoLaunchRunnable);
            instance.autoLaunchRunnable = null;
        }
    }

}
