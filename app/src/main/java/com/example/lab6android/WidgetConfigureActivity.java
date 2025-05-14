package com.example.lab6android;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class WidgetConfigureActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "com.example.dailyphotowidget.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "widget_";

    private EditText customTextEdit;
    private RadioGroup updateFrequencyGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_configure);

        customTextEdit = findViewById(R.id.custom_text_edit);
        updateFrequencyGroup = findViewById(R.id.update_frequency_group);
        Button confirmButton = findViewById(R.id.confirm_button);

        // Отримання ID віджета з Intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Якщо не вдалося отримати ID, закриваємо активність
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = WidgetConfigureActivity.this;

                // Зберігаємо налаштування користувача
                String customText = customTextEdit.getText().toString();
                if (!customText.isEmpty()) {
                    saveCustomText(context, appWidgetId, customText);
                }

                // Зберігаємо вибрану частоту оновлення
                int selectedFrequency = 1; // За замовчуванням щодня
                int checkedId = updateFrequencyGroup.getCheckedRadioButtonId();

                if (checkedId == R.id.radio_daily) {
                    selectedFrequency = 1;
                } else if (checkedId == R.id.radio_12hours) {
                    selectedFrequency = 2;
                } else if (checkedId == R.id.radio_hourly) {
                    selectedFrequency = 24;
                }
                saveUpdateFrequency(context, appWidgetId, selectedFrequency);

                // Оновлюємо віджет з новими налаштуваннями
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                Intent updateIntent = new Intent(context, MyWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
                sendBroadcast(updateIntent);

                // Встановлюємо результат і закриваємо активність
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    // Методи для збереження налаштувань
    static void saveCustomText(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "_text", text);
        prefs.apply();
    }

    static String loadCustomText(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId + "_text", "");
    }

    static void saveUpdateFrequency(Context context, int appWidgetId, int frequency) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_frequency", frequency);
        prefs.apply();
    }

    static int loadUpdateFrequency(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_frequency", 1); // За замовчуванням щодня
    }

    static void deleteWidgetPrefs(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_text");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_frequency");
        prefs.apply();
    }
}