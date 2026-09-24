package org.clothingstore.util;

import org.clothingstore.config.SupabaseConfig;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {

    /**
     * Загружает изображение по HTTPS через HttpURLConnection.
     * JavaFX не умеет сам загружать HTTPS-картинки — используем InputStream.
     */
    public static Image loadFromUrl(String imageUrl,
                                    double width,
                                    double height) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        // ★ User-Agent обязателен — без него Supabase/CDN может вернуть 403
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.connect();

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code + " для " + imageUrl);
        }

        try (InputStream is = conn.getInputStream()) {
            // Передаём поток напрямую в JavaFX Image — это работает всегда
            Image img = new Image(is, width, height, true, true);
            if (img.isError()) throw new RuntimeException(
                    "Ошибка декодирования изображения");
            return img;
        }
    }

    /**
     * Загружает с дефолтным размером (для карточки)
     */
    public static Image loadCard(String imageUrl) throws Exception {
        return loadFromUrl(imageUrl, 240, 200);
    }

    /**
     * Загружает в высоком качестве (для панели деталей)
     */
    public static Image loadDetail(String imageUrl) throws Exception {
        return loadFromUrl(imageUrl, 440, 280);
    }

    /**
     * Загружает превью (для AdminController)
     */
    public static Image loadPreview(String imageUrl) throws Exception {
        return loadFromUrl(imageUrl, 200, 120);
    }
}
