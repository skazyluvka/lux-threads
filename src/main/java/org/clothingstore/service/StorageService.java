package org.clothingstore.service;

import org.clothingstore.config.SupabaseConfig;
import org.clothingstore.util.SessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

public class StorageService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String uploadProductImage(Path filePath) throws Exception {
        String fileName  = generateFileName(filePath);
        String uploadUrl = SupabaseConfig.STORAGE_URL
                + "/object/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;

        String mimeType  = detectMimeType(filePath);
        byte[] fileBytes = Files.readAllBytes(filePath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("apikey",        SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer " + SessionManager.getAccessToken())
                .header("Content-Type",  mimeType)
                .header("x-upsert",      "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = HTTP.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return SupabaseConfig.getImagePublicUrl(fileName);
        } else {
            throw new RuntimeException(
                    "Ошибка загрузки: HTTP " + response.statusCode()
                            + " → " + response.body());
        }
    }

    public void deleteImage(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isBlank()) return;
        String fileName  = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String deleteUrl = SupabaseConfig.STORAGE_URL
                + "/object/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("apikey",        SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer " + SessionManager.getAccessToken())
                .DELETE()
                .build();

        HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String generateFileName(Path filePath) {
        String original = filePath.getFileName().toString();
        String ext      = original.contains(".")
                ? original.substring(original.lastIndexOf(".")) : ".jpg";
        return UUID.randomUUID().toString() + ext;
    }

    private String detectMimeType(Path filePath) {
        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }
}
