package com.example.lab6android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class ImageTextGenerator {

    private Context context;
    private Random random;

    public ImageTextGenerator(Context context) {
        this.context = context;
        this.random = new Random();
    }

    /**
     * Створює зображення з текстом та зберігає його у внутрішній пам'яті
     * @param text текст для відображення на зображенні
     * @return шлях до файлу зображення
     */
    public String createImageWithText(String text) {
        // Отримуємо випадкове зображення з ресурсів
        TypedArray drawableArray = context.getResources().obtainTypedArray(R.array.widget_backgrounds);
        int drawableId = drawableArray.getResourceId(random.nextInt(drawableArray.length()), 0);
        drawableArray.recycle();

        Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);

        // Створюємо копію зображення для малювання
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        // Налаштовуємо текст
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(bitmap.getHeight() / 12f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setShadowLayer(5f, 2f, 2f, Color.BLACK);

        // Підготовка тексту для відображення
        String displayText = !TextUtils.isEmpty(text) ? text : getRandomQuote();

        // Створюємо багаторядковий текст
        int textWidth = bitmap.getWidth() - 60;
        StaticLayout textLayout = new StaticLayout(
                displayText, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER,
                1.0f, 0.0f, false);

        // Розміщуємо текст внизу зображення
        int textX = (bitmap.getWidth() - textWidth) / 2;
        int textY = bitmap.getHeight() - textLayout.getHeight() - 40;

        // Малюємо напівпрозорий фон для тексту
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(150);
        Rect backgroundRect = new Rect(
                0,
                textY - 20,
                bitmap.getWidth(),
                bitmap.getHeight());
        canvas.drawRect(backgroundRect, backgroundPaint);

        // Малюємо текст
        canvas.save();
        canvas.translate(textX, textY);
        textLayout.draw(canvas);
        canvas.restore();

        // Зберігаємо зображення у внутрішній пам'яті
        File imageFile = new File(context.getFilesDir(), "widget_image_" + System.currentTimeMillis() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // Звільняємо ресурси
            bitmap.recycle();
            originalBitmap.recycle();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Повертає випадкову цитату або мотиваційний текст
     */
    private String getRandomQuote() {
        String[] quotes = {
                "Кожен новий день - це нова можливість!",
                "Мрій, вір, досягай!",
                "Найкращий час для нових починань - зараз!",
                "Успіх приходить до тих, хто діє!",
                "Твоя посмішка змінює світ навколо!",
                "Будь кращою версією себе сьогодні!",
                "Неможливе можливо, якщо ти віриш!",
                "Наповни день яскравими моментами!",
                "Маленькі кроки ведуть до великих перемог!",
                "Живи мрією!"
        };

        return quotes[random.nextInt(quotes.length)];
    }
}