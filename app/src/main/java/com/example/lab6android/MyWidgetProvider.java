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
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Random;

public class MyWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_UPDATE_WIDGET = "com.example.lab6android.UPDATE_WIDGET";

    // Масив з ID зображень
    private static final int[] IMAGES = {
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
            R.drawable.image4,
            R.drawable.image5
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

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
        for (int appWidgetId : appWidgetIds) {
            // Перевіряємо чи є власний текст для віджета
            String customText = WidgetConfigureActivity.loadCustomText(context, appWidgetId);

            // Отримуємо масиви текстів та зображень з ресурсів
            String[] texts = context.getResources().getStringArray(R.array.widget_quotes);
            TypedArray drawableArray = context.getResources().obtainTypedArray(R.array.widget_backgrounds);

            // Генеруємо випадковий індекс для зображення та тексту
            Random random = new Random();
            int imageIndex = random.nextInt(drawableArray.length());
            int textIndex = random.nextInt(texts.length);

            // Отримуємо ресурс зображення
            int imageResId = drawableArray.getResourceId(imageIndex, R.drawable.image1);

            // Звільняємо ресурси
            drawableArray.recycle();

            // Використовуємо власний текст, якщо він є
            String displayText = customText.isEmpty() ? texts[textIndex] : customText;

            // Оновлюємо вміст віджета
            updateAppWidget(context, appWidgetManager, appWidgetId, imageResId, displayText);
        }

        // Встановлюємо будильник для наступного оновлення
        scheduleNextUpdate(context);
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
        // Створюємо RemoteViews об'єкт
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Оновлюємо зображення та текст
        views.setImageViewResource(R.id.widget_image, imageResId);
        views.setTextViewText(R.id.widget_text, text);

        // Встановлюємо онклік на весь віджет для ручного оновлення
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // Оновлюємо віджет
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Метод для встановлення будильника на наступне оновлення
    private void scheduleNextUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Отримуємо налаштування частоти оновлення (у хвилинах)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int updateFrequencyMinutes = Integer.parseInt(prefs.getString("update_frequency", "1440")); // За замовчуванням - раз на день

        // Встановлюємо час наступного оновлення
        Calendar calendar = Calendar.getInstance();

        // Якщо щоденне оновлення, встановлюємо на 00:01 наступного дня
        if (updateFrequencyMinutes >= 1440) { // Якщо день або більше
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else {
            // Інакше додаємо вказану кількість хвилин до поточного часу
            calendar.add(Calendar.MINUTE, updateFrequencyMinutes);
        }

        // Встановлюємо повторюваний будильник
        alarmManager.setExact(
                AlarmManager.RTC,
                calendar.getTimeInMillis(),
                getPendingIntent(context)
        );
    }

    // Створення PendingIntent для будильника
    private PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}