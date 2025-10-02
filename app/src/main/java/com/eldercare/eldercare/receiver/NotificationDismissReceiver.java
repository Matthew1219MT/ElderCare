package com.eldercare.eldercare.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eldercare.eldercare.service.FallDetectionService;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // User dismissed the notification → cancel auto-launch
        FallDetectionService.cancelAutoLaunch();
    }
}