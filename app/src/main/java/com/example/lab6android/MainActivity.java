package com.example.lab6android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentResolver;
import java.io.InputStream;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "com.example.lab6android.WidgetContentPrefs";
    private static final String PREF_ALL_TEXTS = "all_texts";
    private static final String PREF_CUSTOM_IMAGES = "custom_images";
    private static final String PREF_ENABLED_IMAGES = "enabled_images";

    private static final int REQUEST_PICK_IMAGE = 1001;

    private TabLayout tabLayout;
    private ListView contentListView;
    private FloatingActionButton fabAdd;

    private List<String> textsList = new ArrayList<>();
    private List<ImageItem> imagesList = new ArrayList<>();

    private TextsAdapter textsAdapter;
    private ImagesAdapter imagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ініціалізація UI компонентів
        tabLayout = findViewById(R.id.tab_layout);
        contentListView = findViewById(R.id.content_list);
        fabAdd = findViewById(R.id.fab_add);

        // Завантажуємо дані
        loadAllTexts();
        loadImageItems();

        // Створюємо адаптери
        textsAdapter = new TextsAdapter(this, textsList);
        imagesAdapter = new ImagesAdapter(this, imagesList);

        // Встановлюємо початковий адаптер
        contentListView.setAdapter(textsAdapter);

        // Налаштовуємо обробники подій для TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Вкладка з текстами
                    contentListView.setAdapter(textsAdapter);
                } else {
                    // Вкладка з зображеннями
                    contentListView.setAdapter(imagesAdapter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Налаштовуємо кнопку додавання
        fabAdd.setOnClickListener(v -> {
            if (tabLayout.getSelectedTabPosition() == 0) {
                // Додавання нового тексту
                showAddTextDialog();
            } else {
                // Додавання нового зображення
                showAddImageDialog();
            }
        });
    }

    // Метод для завантаження всіх текстів (і стандартних, і користувацьких)
    private void loadAllTexts() {
        try {
            // Завантажуємо всі тексти зі SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> savedTexts = prefs.getStringSet(PREF_ALL_TEXTS, null);

            if (savedTexts == null || savedTexts.isEmpty()) {
                // Якщо немає збережених текстів, завантажуємо стандартні з ресурсів
                String[] defaultTexts = getResources().getStringArray(R.array.widget_quotes);
                textsList.addAll(Arrays.asList(defaultTexts));

                // І одразу зберігаємо їх в SharedPreferences для майбутнього редагування
                saveAllTexts();
            } else {
                // Якщо є збережені тексти, використовуємо їх
                textsList.addAll(savedTexts);
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка при завантаженні текстів", e);
            // Завантажуємо запасні тексти, якщо виникла помилка
            textsList.add("Гарного дня!");
            textsList.add("Чудового настрою!");
        }
    }

    // Метод для збереження всіх текстів
    private void saveAllTexts() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> allTexts = new HashSet<>(textsList);
            prefs.edit().putStringSet(PREF_ALL_TEXTS, allTexts).apply();
        } catch (Exception e) {
            Log.e(TAG, "Помилка при збереженні текстів", e);
            Toast.makeText(this, "Помилка при збереженні текстів", Toast.LENGTH_SHORT).show();
        }
    }

    // Метод для завантаження зображень
    private void loadImageItems() {
        try {
            // Очищаємо попередній список
            imagesList.clear();

            // Завантажуємо стандартні зображення
            int[] defaultResIds = {
                    R.drawable.image1,
                    R.drawable.image2,
                    R.drawable.image3,
                    R.drawable.image4,
                    R.drawable.image5
            };

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> enabledImages = prefs.getStringSet(PREF_ENABLED_IMAGES, new HashSet<>());
            Set<String> customImages = prefs.getStringSet(PREF_CUSTOM_IMAGES, new HashSet<>());

            // Додаємо стандартні зображення
            for (int i = 0; i < defaultResIds.length; i++) {
                String imageId = "default_" + i;
                String imageName = "Image " + (i + 1);
                boolean isEnabled = enabledImages.isEmpty() || enabledImages.contains(imageId);

                ImageItem item = new ImageItem(defaultResIds[i], imageName, isEnabled, imageId, true);
                imagesList.add(item);
            }

            // Додаємо користувацькі зображення
            for (String imageData : customImages) {
                try {
                    String[] parts = imageData.split("\\|");
                    if (parts.length >= 3) {
                        String imageId = parts[0];
                        String imageUriStr = parts[1];
                        String imageName = parts[2];
                        boolean isEnabled = enabledImages.isEmpty() || enabledImages.contains(imageId);

                        Uri imageUri = Uri.parse(imageUriStr);
                        // Перевіряємо URI перед додаванням
                        boolean isUriValid = false;
                        try {
                            ContentResolver resolver = getContentResolver();
                            try (InputStream is = resolver.openInputStream(imageUri)) {
                                isUriValid = (is != null);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Не вдалося перевірити URI: " + imageUri, e);
                        }

                        if (isUriValid) {
                            ImageItem item = new ImageItem(imageUri, imageName, isEnabled, imageId, false);
                            imagesList.add(item);
                            Log.d(TAG, "Додано користувацьке зображення: " + imageName);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Помилка при обробці користувацького зображення", e);
                }
            }

            Log.d(TAG, "Завантажено загалом " + imagesList.size() + " зображень");
        } catch (Exception e) {
            Log.e(TAG, "Помилка при завантаженні зображень", e);
        }
    }

    // Метод для збереження налаштувань зображень
    private void saveImageSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Set<String> enabledImages = new HashSet<>();
            Set<String> customImages = new HashSet<>();

            // Зберігаємо інформацію про включені зображення та користувацькі зображення
            for (ImageItem item : imagesList) {
                if (item.isEnabled) {
                    enabledImages.add(item.id);
                }

                // Зберігаємо інформацію про користувацькі зображення
                if (!item.isDefault && item.uri != null) {
                    // Перед збереженням перевіримо доступність URI
                    boolean isUriValid = false;
                    try {
                        InputStream is = getContentResolver().openInputStream(item.uri);
                        if (is != null) {
                            is.close();
                            isUriValid = true;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Пропускаємо недійсний URI: " + item.uri, e);
                    }

                    if (isUriValid) {
                        String imageData = item.id + "|" + item.uri.toString() + "|" + item.name;
                        customImages.add(imageData);
                        Log.d(TAG, "Збережено зображення: " + item.name + " з URI: " + item.uri);
                    }
                }
            }

            // Виводимо інформацію для діагностики
            Log.d(TAG, "Збережено " + enabledImages.size() + " включених зображень");
            Log.d(TAG, "Збережено " + customImages.size() + " користувацьких зображень");

            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(PREF_ENABLED_IMAGES, enabledImages);
            editor.putStringSet(PREF_CUSTOM_IMAGES, customImages);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Помилка при збереженні налаштувань зображень", e);
            Toast.makeText(this, "Помилка при збереженні налаштувань", Toast.LENGTH_SHORT).show();
        }
    }

    // Діалог для додавання нового тексту
    private void showAddTextDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_text, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.edit_text);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(v -> {
            String newText = editText.getText().toString().trim();
            if (!newText.isEmpty()) {
                textsList.add(newText);
                textsAdapter.notifyDataSetChanged();
                saveAllTexts();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Текст додано", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Текст не може бути порожнім", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Діалог для вибору зображення з галереї
    private void showAddImageDialog() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "Помилка при відкритті галереї", e);
            Toast.makeText(this, "Помилка при відкритті галереї", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    // Логуємо отриманий URI для діагностики
                    Log.d(TAG, "Отримано URI зображення: " + selectedImageUri.toString());

                    try {
                        // Пробуємо отримати постійний доступ з обробкою можливих винятків
                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;

                        // Перевіряємо схему URI
                        if (ContentResolver.SCHEME_CONTENT.equals(selectedImageUri.getScheme())) {
                            try {
                                getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
                                Log.d(TAG, "Успішно отримано persistable URI permission");
                            } catch (SecurityException se) {
                                Log.w(TAG, "Не вдалося отримати persistable URI permission: " + se.getMessage());
                                // Продовжуємо роботу навіть без постійного дозволу
                            }
                        } else {
                            Log.w(TAG, "URI не має схеми content://, пропускаємо takePersistableUriPermission");
                        }

                        // Додаємо перевірку на правильність URI
                        boolean isValidUri = false;
                        try {
                            // Пробуємо отримати потік для перевірки дійсності URI
                            InputStream is = getContentResolver().openInputStream(selectedImageUri);
                            if (is != null) {
                                is.close();
                                isValidUri = true;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "URI недійсний: " + e.getMessage());
                        }

                        if (isValidUri) {
                            // Запитуємо ім'я для зображення
                            showImageNameDialog(selectedImageUri);
                        } else {
                            Toast.makeText(this, "Не вдалося отримати доступ до вибраного зображення",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Помилка при роботі з URI зображення", e);
                        Toast.makeText(this, "Помилка при обробці URI зображення", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Не вдалося отримати URI зображення", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Загальна помилка при обробці вибраного зображення", e);
            Toast.makeText(this, "Помилка при обробці зображення", Toast.LENGTH_SHORT).show();
        }
    }

    // Діалог для введення назви зображення
    private void showImageNameDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_text, null);
        builder.setView(dialogView);

        TextView titleView = dialogView.findViewById(R.id.edit_text_title);
        if (titleView != null) {
            titleView.setText(R.string.enter_image_name);
        }

        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setHint(R.string.image_name_hint);

        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(v -> {
            String imageName = editText.getText().toString().trim();
            if (!imageName.isEmpty()) {
                // Створюємо унікальний ідентифікатор для зображення
                String imageId = "custom_" + System.currentTimeMillis();

                // Додаємо нове зображення
                ImageItem newImage = new ImageItem(imageUri, imageName, true, imageId, false);
                imagesList.add(newImage);
                imagesAdapter.notifyDataSetChanged();

                // Зберігаємо налаштування
                saveImageSettings();

                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Зображення додано", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Назва не може бути порожньою", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Діалог для редагування існуючого тексту
    private void showEditTextDialog(int position, String currentText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_text, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.edit_text);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Змінюємо текст кнопки на "Оновити"
        btnAdd.setText(R.string.update);

        // Встановлюємо поточний текст
        editText.setText(currentText);

        AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(v -> {
            String newText = editText.getText().toString().trim();
            if (!newText.isEmpty()) {
                textsList.set(position, newText);
                textsAdapter.notifyDataSetChanged();
                saveAllTexts();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Текст оновлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Текст не може бути порожнім", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Клас елемента зображення
    private static class ImageItem {
        int resourceId; // Для стандартних зображень
        Uri uri;        // Для користувацьких зображень
        String name;
        boolean isEnabled;
        String id;      // Унікальний ідентифікатор
        boolean isDefault; // Чи є зображення стандартним

        // Конструктор для стандартних зображень
        ImageItem(int resourceId, String name, boolean isEnabled, String id, boolean isDefault) {
            this.resourceId = resourceId;
            this.name = name;
            this.isEnabled = isEnabled;
            this.id = id;
            this.isDefault = isDefault;
        }

        // Конструктор для користувацьких зображень
        ImageItem(Uri uri, String name, boolean isEnabled, String id, boolean isDefault) {
            this.uri = uri;
            this.name = name;
            this.isEnabled = isEnabled;
            this.id = id;
            this.isDefault = isDefault;
            this.resourceId = 0; // Не використовується для користувацьких зображень
        }
    }

    // Адаптер для списку текстів
    private class TextsAdapter extends ArrayAdapter<String> {
        private final Context context;

        public TextsAdapter(Context context, List<String> texts) {
            super(context, 0, texts);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_text, parent, false);
            }

            String text = getItem(position);
            if (text == null) {
                return convertView;
            }

            TextView textView = convertView.findViewById(R.id.text_content);
            Button btnEdit = convertView.findViewById(R.id.btn_edit);
            Button btnDelete = convertView.findViewById(R.id.btn_delete);

            textView.setText(text);

            // Обробник для кнопки редагування
            btnEdit.setOnClickListener(v -> showEditTextDialog(position, text));

            // Обробник для кнопки видалення
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.delete_confirmation)
                        .setMessage(R.string.delete_text_confirm)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            textsList.remove(position);
                            notifyDataSetChanged();
                            saveAllTexts();
                            Toast.makeText(context, R.string.text_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            });

            return convertView;
        }
    }

    // Адаптер для списку зображень
    private class ImagesAdapter extends ArrayAdapter<ImageItem> {
        private final Context context;

        public ImagesAdapter(Context context, List<ImageItem> images) {
            super(context, 0, images);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
                }

                ImageItem imageItem = getItem(position);
                if (imageItem == null) {
                    return convertView;
                }

                ImageView imageView = convertView.findViewById(R.id.image_preview);
                TextView textView = convertView.findViewById(R.id.image_name);
                androidx.appcompat.widget.SwitchCompat enableSwitch = convertView.findViewById(R.id.switch_enable);
                Button btnDelete = convertView.findViewById(R.id.btn_delete_image);

                // Очищаємо попереднє зображення
                imageView.setImageDrawable(null);

                // Встановлюємо зображення в залежності від типу
                try {
                    if (imageItem.isDefault) {
                        imageView.setImageResource(imageItem.resourceId);
                        // Стандартні зображення не можна видалити
                        btnDelete.setVisibility(View.GONE);
                    } else if (imageItem.uri != null) {
                        // Перевіряємо URI перед відображенням
                        boolean canLoadUri = false;
                        try (InputStream is = context.getContentResolver().openInputStream(imageItem.uri)) {
                            canLoadUri = (is != null);
                        } catch (Exception e) {
                            Log.w(TAG, "Не вдалося перевірити URI: " + imageItem.uri, e);
                        }

                        if (canLoadUri) {
                            // Використовуємо безпечний метод завантаження URI
                            try {
                                // Вимикаємо кеш для уникнення проблем із пам'яттю
                                imageView.setImageURI(null);
                                imageView.setImageURI(imageItem.uri);
                                Log.d(TAG, "URI зображення встановлено успішно: " + imageItem.uri);
                            } catch (Exception e) {
                                Log.e(TAG, "Помилка при встановленні URI: " + imageItem.uri, e);
                                imageView.setImageResource(R.drawable.image1);
                            }
                        } else {
                            // Якщо URI недійсний, використовуємо запасне зображення
                            Log.w(TAG, "Недійсний URI: " + imageItem.uri + ", використовуємо запасне зображення");
                            imageView.setImageResource(R.drawable.image1);
                        }
                        // Користувацькі зображення можна видалити
                        btnDelete.setVisibility(View.VISIBLE);
                    } else {
                        // Встановлюємо запасне зображення якщо URI некоректний
                        imageView.setImageResource(R.drawable.image1);
                        btnDelete.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Помилка при встановленні зображення", e);
                    // Встановлюємо запасне зображення у випадку помилки
                    imageView.setImageResource(R.drawable.image1);
                }

                textView.setText(imageItem.name != null ? imageItem.name : "Невідоме зображення");
                enableSwitch.setChecked(imageItem.isEnabled);

                enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    imageItem.isEnabled = isChecked;
                    saveImageSettings();
                });

                // Обробка видалення користувацького зображення
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.delete_confirmation)
                            .setMessage(R.string.delete_image_confirm)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                imagesList.remove(position);
                                notifyDataSetChanged();
                                saveImageSettings();
                                Toast.makeText(context, R.string.image_deleted, Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                });

                return convertView;
            } catch (Exception e) {
                Log.e(TAG, "Помилка при створенні елемента списку зображень", e);
                // Повертаємо порожній View у випадку помилки
                if (convertView == null) {
                    return new View(context);
                }
                return convertView;
            }
        }
    }
}