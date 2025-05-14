package com.example.lab6android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.util.Log;

/**
 * Приймач для обробки подій перезавантаження пристрою
 * Запускає оновлення всіх віджетів після перезавантаження
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Пристрій перезавантажено. Оновлюємо всі віджети.");

            try {
                // Отримуємо менеджер віджетів
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                // Отримуємо ID всіх активних віджетів
                ComponentName widgetComponent = new ComponentName(context, MyWidgetProvider.class);
                int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

                if (widgetIds != null && widgetIds.length > 0) {
                    // Запускаємо оновлення віджетів
                    Intent updateIntent = new Intent(context, MyWidgetProvider.class);
                    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                    context.sendBroadcast(updateIntent);

                    Log.d(TAG, "Запит на оновлення " + widgetIds.length + " віджетів надіслано.");
                } else {
                    Log.d(TAG, "Активних віджетів не знайдено.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Помилка при спробі оновити віджети після перезавантаження", e);
            }
        }
    }
}