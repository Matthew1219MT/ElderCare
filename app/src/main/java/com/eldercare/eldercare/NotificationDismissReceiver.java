package com.eldercare.eldercare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // User dismissed the notification → cancel auto-launch
        FallDetectionService.cancelAutoLaunch();
    }
}