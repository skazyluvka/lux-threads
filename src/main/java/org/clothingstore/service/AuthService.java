package org.clothingstore.service;

import org.clothingstore.model.UserProfile;
import org.clothingstore.util.SessionManager;
import com.google.gson.*;

import java.util.HashMap;
import java.util.Map;

public class AuthService extends SupabaseClient {

    public boolean login(String email, String password) throws Exception {
        Map<String,String> body = new HashMap<>();
        body.put("email",    email);
        body.put("password", password);
        String response = authPost("/token?grant_type=password", body);
        JsonObject json = GSON.fromJson(response, JsonObject.class);
        if (json.has("access_token")) {
            String token = json.get("access_token").getAsString();
            String uid   = json.getAsJsonObject("user").get("id").getAsString();

            // ✅ Отладка: выводим в консоль, что логин прошёл успешно
            System.out.println("✅ УСПЕШНЫЙ ВХОД! User ID: " + uid);
            System.out.println("📧 Email: " + email);

            SessionManager.login(token, uid, email);
            loadProfile(uid);
            return true;
        }
        return false;
    }

    public boolean register(String email, String password) throws Exception {
        Map<String,String> body = new HashMap<>();
        body.put("email",    email);
        body.put("password", password);
        String response = authPost("/signup", body);
        JsonObject json = GSON.fromJson(response, JsonObject.class);
        if (json.has("access_token")) {
            String token = json.get("access_token").getAsString();
            String uid   = json.getAsJsonObject("user").get("id").getAsString();

            // ✅ Отладка
            System.out.println("✅ УСПЕШНАЯ РЕГИСТРАЦИЯ! User ID: " + uid);

            SessionManager.login(token, uid, email);
            createProfile(uid);
            return true;
        } else if (json.has("id")) {
            // email confirmation required — just success
            return true;
        }
        return false;
    }

    public void sendPasswordReset(String email) throws Exception {
        Map<String,String> body = new HashMap<>();
        body.put("email", email);
        authPost("/recover", body);
    }

    private void loadProfile(String uid) {
        try {
            String res = get("/user_profiles?id=eq." + uid + "&select=*");
            JsonArray arr = GSON.fromJson(res, JsonArray.class);
            if (arr.size() > 0) {
                UserProfile p = GSON.fromJson(arr.get(0), UserProfile.class);
                SessionManager.setProfile(p);
                System.out.println(" Профиль загружен: " + p.getFullName());
            } else {
                System.out.println("⚠️ Профиль не найден, создаём новый...");
                createProfile(uid);
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка загрузки профиля: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createProfile(String uid) {
        try {
            Map<String,String> body = new HashMap<>();
            body.put("id",   uid);
            body.put("role", "user");
            String res = post("/user_profiles", body);
            JsonArray arr = GSON.fromJson(res, JsonArray.class);
            if (arr.size() > 0) {
                SessionManager.setProfile(GSON.fromJson(arr.get(0), UserProfile.class));
                System.out.println("✅ Профиль создан успешно");
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка создания профиля: " + e.getMessage());
            e.printStackTrace();
        }
    }
}