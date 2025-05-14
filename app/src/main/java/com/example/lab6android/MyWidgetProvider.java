package com.example.lab6android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MyWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "MyWidgetProvider";
    public static final String ACTION_UPDATE_WIDGET = "com.example.lab6android.UPDATE_WIDGET";

    // Константи для SharedPreferences, які мають збігатися з MainActivity
    private static final String PREFS_NAME = "com.example.lab6android.WidgetContentPrefs";
    private static final String PREF_ALL_TEXTS = "all_texts";
    private static final String PREF_ENABLED_IMAGES = "enabled_images";
    private static final String PREF_CUSTOM_IMAGES = "custom_images";

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

                // Отримуємо масиви текстів з SharedPreferences
                List<String> availableTexts = loadAvailableTexts(context);
                if (availableTexts.isEmpty()) {
                    Log.w(TAG, "Немає доступних текстів, використовуємо запасний варіант");
                    availableTexts.add("Гарного дня!");
                    availableTexts.add("Чудового настрою!");
                }

                // Генеруємо випадковий індекс для зображення та тексту
                Random random = new Random();
                int textIndex = random.nextInt(availableTexts.size());

                // Отримуємо інформацію про доступні зображення
                List<Integer> defaultImageResourceIds = new ArrayList<>();
                List<Uri> customImageUris = new ArrayList<>();
                loadAvailableImages(context, defaultImageResourceIds, customImageUris);

                // Перевіряємо, чи є доступні зображення
                int imageResId;
                Uri imageUri = null;

                if (!defaultImageResourceIds.isEmpty() || !customImageUris.isEmpty()) {
                    // Вибираємо випадково між стандартними та користувацькими зображеннями
                    boolean useDefault = defaultImageResourceIds.isEmpty() ? false :
                            (customImageUris.isEmpty() ? true : random.nextBoolean());

                    if (useDefault && !defaultImageResourceIds.isEmpty()) {
                        // Використовуємо стандартне зображення
                        int imageIndex = random.nextInt(defaultImageResourceIds.size());
                        imageResId = defaultImageResourceIds.get(imageIndex);
                    } else if (!customImageUris.isEmpty()) {
                        // Використовуємо користувацьке зображення
                        int imageIndex = random.nextInt(customImageUris.size());
                        imageUri = customImageUris.get(imageIndex);
                        imageResId = 0; // Не використовується для Uri
                    } else {
                        // Запасний варіант, якщо щось пішло не так
                        imageResId = R.drawable.image1;
                    }
                } else {
                    // Використовуємо запасне зображення якщо немає доступних
                    imageResId = R.drawable.image1;
                }

                // Використовуємо власний текст, якщо він є
                String displayText = customText.isEmpty() ? availableTexts.get(textIndex) : customText;

                // Оновлюємо вміст віджета
                updateAppWidget(context, appWidgetManager, appWidgetId, imageResId, imageUri, displayText);
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

    // Завантаження усіх доступних текстів з SharedPreferences
    private List<String> loadAvailableTexts(Context context) {
        List<String> texts = new ArrayList<>();

        // Спочатку спробуємо завантажити з спільного SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedTexts = prefs.getStringSet(PREF_ALL_TEXTS, new HashSet<>());

        if (!savedTexts.isEmpty()) {
            texts.addAll(savedTexts);
        } else {
            // Якщо немає текстів у SharedPreferences, завантажимо з ресурсів
            try {
                String[] defaultTexts = context.getResources().getStringArray(R.array.widget_quotes);
                if (defaultTexts.length > 0) {
                    for (String text : defaultTexts) {
                        texts.add(text);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Помилка при отриманні текстів з ресурсів", e);
            }
        }

        return texts;
    }

    // Завантаження усіх доступних зображень (і стандартних, і користувацьких)
    private void loadAvailableImages(Context context, List<Integer> defaultImagesList, List<Uri> customImagesList) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> enabledImages = prefs.getStringSet(PREF_ENABLED_IMAGES, new HashSet<>());
        Set<String> customImages = prefs.getStringSet(PREF_CUSTOM_IMAGES, new HashSet<>());

        // Спочатку перевіряємо стандартні зображення
        int[] defaultResIds = {
                R.drawable.image1,
                R.drawable.image2,
                R.drawable.image3,
                R.drawable.image4,
                R.drawable.image5
        };

        for (int i = 0; i < defaultResIds.length; i++) {
            String imageId = "default_" + i;
            if (enabledImages.isEmpty() || enabledImages.contains(imageId)) {
                defaultImagesList.add(defaultResIds[i]);
            }
        }

        // Потім завантажуємо користувацькі зображення
        for (String imageData : customImages) {
            try {
                String[] parts = imageData.split("\\|");
                if (parts.length >= 3) {
                    String imageId = parts[0];
                    String imageUriStr = parts[1];

                    if (enabledImages.isEmpty() || enabledImages.contains(imageId)) {
                        Uri imageUri = Uri.parse(imageUriStr);
                        customImagesList.add(imageUri);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Помилка при обробці користувацького зображення", e);
            }
        }
    }

    // Метод для оновлення вмісту віджета
    // Метод для оновлення вмісту віджета
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId, int imageResId, Uri imageUri, String text) {
        try {
            // Створюємо RemoteViews об'єкт
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Оновлюємо зображення в залежності від типу
            boolean imageSet = false;

            // Спочатку спробуємо використовувати URI, якщо він є
            if (imageUri != null) {
                try {
                    // Перевіряємо, чи є URI дійсним
                    boolean isUriValid = false;
                    try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                        isUriValid = (is != null);
                    } catch (Exception e) {
                        Log.w(TAG, "Недійсний URI у віджеті: " + imageUri, e);
                    }

                    if (isUriValid) {
                        views.setImageViewUri(R.id.widget_image, imageUri);
                        imageSet = true;
                        Log.d(TAG, "Встановлено URI зображення для віджета: " + imageUri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Помилка при встановленні URI для віджета", e);
                }
            }

            // Якщо URI не встановлено або виникла помилка, використовуємо ресурс
            if (!imageSet && imageResId > 0) {
                try {
                    views.setImageViewResource(R.id.widget_image, imageResId);
                    imageSet = true;
                    Log.d(TAG, "Встановлено ресурс зображення для віджета: " + imageResId);
                } catch (Exception e) {
                    Log.e(TAG, "Помилка при встановленні ресурсу зображення для віджета", e);
                }
            }

            // Якщо не вдалося встановити ні URI, ні ресурс, використовуємо запасне зображення
            if (!imageSet) {
                views.setImageViewResource(R.id.widget_image, R.drawable.image1);
                Log.d(TAG, "Використовуємо запасне зображення для віджета");
            }

            // Оновлюємо текст
            views.setTextViewText(R.id.widget_text, text);

            // Встановлюємо онклік на весь віджет для ручного оновлення
            Intent intent = new Intent(context, MyWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            // Оновлюємо віджет
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.d(TAG, "Віджет " + appWidgetId + " успішно оновлено");

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
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}