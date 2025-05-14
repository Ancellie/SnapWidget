package com.example.lab6android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Налаштування");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Оновлюємо підписи налаштувань
            ListPreference updateFrequency = findPreference("update_frequency");
            if (updateFrequency != null) {
                updateFrequency.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }

            // Додаємо кнопку для оновлення віджетів
            Preference updateButton = findPreference("update_now");
            if (updateButton != null) {
                updateButton.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(getActivity(), MyWidgetProvider.class);
                    intent.setAction(MyWidgetProvider.ACTION_UPDATE_WIDGET);
                    getActivity().sendBroadcast(intent);
                    return true;
                });
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Оновлюємо віджети при зміні налаштувань
            Intent intent = new Intent(getActivity(), MyWidgetProvider.class);
            intent.setAction(MyWidgetProvider.ACTION_UPDATE_WIDGET);
            getActivity().sendBroadcast(intent);
        }
    }
}