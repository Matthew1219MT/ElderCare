package com.eldercare.eldercare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FallDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private static final float THRESHOLD = 1f;
    private Handler handler = new Handler();
    private Runnable autoLaunchRunnable;
    private static final int NOTIFICATION_ID = 3;

    private static FallDetectionService instance;
    private static final String CHANNEL_ID = "1";

    //Test Image - needs to be REPLACED!!!

    public static void cancelAutoLaunch() {
        android.util.Log.d("CancelAutoDoesRun", "Auto-launch cancelling");
        if (instance != null && instance.autoLaunchRunnable != null) {
            instance.handler.removeCallbacks(instance.autoLaunchRunnable);
            instance.autoLaunchRunnable = null;
            android.util.Log.d("FallDetectionService", "Auto-launch cancelled");
        }
    }

    /*private final BroadcastReceiver notificationTapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NOTIFICATION_TAP.equals(intent.getAction())) {
                // Cancel pending auto-launch
                if (autoLaunchRunnable != null) {
                    handler.removeCallbacks(autoLaunchRunnable);
                    autoLaunchRunnable = null;
                }

                // Open MainActivity with dialog
                Intent i = new Intent(context, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("SHOW_DIALOG", true);
                context.startActivity(i);
            }
        }
    };*/

    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        createNotificationChannel();

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //registerReceiver(notificationTapReceiver, new IntentFilter(ACTION_NOTIFICATION_TAP), Context.RECEIVER_NOT_EXPORTED);

        // Start foreground notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "fall_channel")
                .setContentTitle("Fall Detection Active")
                .setContentText("Monitoring for falls in the background")
                .setSmallIcon(R.drawable.warning);

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
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        if (magnitude > THRESHOLD) {
            sendFallNotification();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendFallNotification() {
        cancelAutoLaunch();
        Bitmap bitmap_image = BitmapFactory.decodeResource(this.getResources(),R.drawable.bmp_24);

        // Check if app is in foreground
        if (isAppInForeground()) {
            // App is open → directly launch the dialog
            Intent dialogIntent = new Intent(this, MainActivity.class);
            dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            dialogIntent.putExtra("SHOW_DIALOG", true);
            startActivity(dialogIntent);
            startActivity(dialogIntent);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Intent to open MainActivity with dialog
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityIntent.putExtra("SHOW_DIALOG", true);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent to detect dismiss
        Intent dismissIntent = new Intent(this, NotificationDismissReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 1️⃣ First notification (tap = open, dismiss = nothing, autoCancel enabled)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "fall_channel")
                .setSmallIcon(R.drawable.warning)
                .setLargeIcon(bitmap_image)
                .setContentTitle("Fall Detected")
                .setContentText("A fall has been detected!")
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(activityPendingIntent)
                .setDeleteIntent(dismissPendingIntent);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());


        // Schedule auto-launch in 10 seconds if no action
        autoLaunchRunnable = () -> {
            NotificationCompat.Builder updatedBuilder = new NotificationCompat.Builder(this, "fall_channel")
                    .setSmallIcon(R.drawable.warning)
                    .setContentTitle("Fall Detected")
                    .setContentText("No response detected! Opening app...")
                    .setFullScreenIntent(activityPendingIntent, true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(activityPendingIntent)
                    .setFullScreenIntent(activityPendingIntent, true);;

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, updatedBuilder.build());

            android.util.Log.d("CompleteSecondNotif", "Ran Second Notif");
        };

        handler.postDelayed(autoLaunchRunnable, 10_000);
    }
    private boolean isAppInForeground() {
        android.app.ActivityManager.RunningAppProcessInfo appProcessInfo =
                new android.app.ActivityManager.RunningAppProcessInfo();
        android.app.ActivityManager.getMyMemoryState(appProcessInfo);
        return appProcessInfo.importance ==
                android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

}
