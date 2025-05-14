// Виправлений код для MyWidgetProvider.java
package com.example.lab6android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.Log;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Random;

public class MyWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "MyWidgetProvider";
    public static final String ACTION_UPDATE_WIDGET = "com.example.lab6android.UPDATE_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d(TAG, "onReceive: " + intent.getAction());

        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            // Оновлюємо віджет коли спрацьовує будильник
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            if (appWidgetIds != null && appWidgetIds.length > 0) {
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            Log.d(TAG, "onUpdate: оновлення " + appWidgetIds.length + " віджетів");

            for (int appWidgetId : appWidgetIds) {
                // Перевіряємо чи є власний текст для віджета
                String customText = WidgetConfigureActivity.loadCustomText(context, appWidgetId);
                Log.d(TAG, "onUpdate: customText = " + customText);

                // Отримуємо масиви текстів та зображень з ресурсів
                String[] texts;
                try {
                    texts = context.getResources().getStringArray(R.array.widget_quotes);
                } catch (Exception e) {
                    Log.e(TAG, "Помилка при отриманні масиву текстів", e);
                    texts = new String[]{"Гарного дня!", "Чудового настрою!"};
                }

                // Генеруємо випадковий індекс для зображення та тексту
                Random random = new Random();
                int imageIndex = random.nextInt(5); // Використовуємо фіксовану кількість зображень
                int textIndex = random.nextInt(texts.length);

                // Отримуємо ресурс зображення з фіксованого масиву
                int[] imageResources = {
                        R.drawable.image1,
                        R.drawable.image2,
                        R.drawable.image3,
                        R.drawable.image4,
                        R.drawable.image5
                };
                int imageResId = imageResources[imageIndex];

                // Використовуємо власний текст, якщо він є
                String displayText = customText.isEmpty() ? texts[textIndex] : customText;

                // Оновлюємо вміст віджета
                updateAppWidget(context, appWidgetManager, appWidgetId, imageResId, displayText);
            }

            // Встановлюємо будильник для наступного оновлення
            scheduleNextUpdate(context);

        } catch (Exception e) {
            Log.e(TAG, "Помилка під час оновлення віджета", e);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // Запускаємо будильник коли віджет додано
        scheduleNextUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Відміняємо будильник коли видалено останній віджет
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    // Метод для оновлення вмісту віджета
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId, int imageResId, String text) {
        try {
            // Створюємо RemoteViews об'єкт
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Оновлюємо зображення та текст
            views.setImageViewResource(R.id.widget_image, imageResId);
            views.setTextViewText(R.id.widget_text, text);

            // Встановлюємо онклік на весь віджет для ручного оновлення
            Intent intent = new Intent(context, MyWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            // Оновлюємо віджет
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, "Помилка при оновленні віджета " + appWidgetId, e);
        }
    }

    // Метод для встановлення будильника на наступне оновлення
    private void scheduleNextUpdate(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager недоступний");
                return;
            }

            // Отримуємо ID віджетів
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            if (appWidgetIds == null || appWidgetIds.length == 0) {
                Log.d(TAG, "Немає активних віджетів для оновлення");
                return;
            }

            // Для спрощення беремо частоту оновлення з першого віджета
            int frequency = WidgetConfigureActivity.loadUpdateFrequency(context, appWidgetIds[0]);
            int updateFrequencyMinutes;

            // Конвертуємо значення частоти в хвилини
            switch (frequency) {
                case 2: // Кожні 12 годин
                    updateFrequencyMinutes = 12 * 60;
                    break;
                case 24: // Щогодини
                    updateFrequencyMinutes = 60;
                    break;
                default: // Щоденно (за замовчуванням)
                    updateFrequencyMinutes = 24 * 60;
                    break;
            }

            Log.d(TAG, "Встановлюємо оновлення кожні " + updateFrequencyMinutes + " хвилин");

            // Встановлюємо час наступного оновлення
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, updateFrequencyMinutes);

            Log.d(TAG, "Наступне оновлення о " +
                    calendar.get(Calendar.HOUR_OF_DAY) + ":" +
                    calendar.get(Calendar.MINUTE));

            // Встановлюємо будильник
            PendingIntent pendingIntent = getPendingIntent(context);

            try {
                alarmManager.setExact(
                        AlarmManager.RTC,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Будильник встановлено успішно");
            } catch (SecurityException e) {
                Log.e(TAG, "Немає дозволу на встановлення точного будильника", e);
                // Пробуємо встановити неточний будильник
                alarmManager.set(
                        AlarmManager.RTC,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Помилка при встановленні будильника", e);
        }
    }

    // Створення PendingIntent для будильника
    private PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}