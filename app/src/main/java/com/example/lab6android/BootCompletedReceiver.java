package com.example.lab6android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, rescheduling widget updates");

            // Перезапускаємо оновлення віджетів після перезавантаження пристрою
            Intent updateIntent = new Intent(context, MyWidgetProvider.class);
            updateIntent.setAction(MyWidgetProvider.ACTION_UPDATE_WIDGET);
            context.sendBroadcast(updateIntent);
        }
    }
}