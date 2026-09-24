package org.clothingstore.service;

import org.clothingstore.config.SupabaseConfig;
import org.clothingstore.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SupabaseClient {

    protected static final Gson    GSON   = new GsonBuilder().create();
    private   static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    protected String get(String endpoint) throws Exception {
        HttpRequest req = baseBuilder(endpoint).GET().build();
        return send(req);
    }

    protected String post(String endpoint, Object body) throws Exception {
        String json = GSON.toJson(body);
        HttpRequest req = baseBuilder(endpoint)
                .header("Prefer","return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    protected String patch(String endpoint, Object body) throws Exception {
        String json = GSON.toJson(body);
        HttpRequest req = baseBuilder(endpoint)
                .header("Prefer","return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    protected String delete(String endpoint) throws Exception {
        HttpRequest req = baseBuilder(endpoint)
                .DELETE().build();
        return send(req);
    }

    // Auth-specific POST (different base URL)
    protected String authPost(String path, Object body) throws Exception {
        String json = GSON.toJson(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseConfig.AUTH_URL + path))
                .header("apikey",        SupabaseConfig.ANON_KEY)
                .header("Content-Type",  "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return send(req);
    }

    private HttpRequest.Builder baseBuilder(String endpoint) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseConfig.REST_URL + endpoint))
                .header("apikey",       SupabaseConfig.ANON_KEY)
                .header("Content-Type", "application/json");
        if (SessionManager.isLoggedIn())
            b.header("Authorization", "Bearer " + SessionManager.getAccessToken());
        return b;
    }

    private String send(HttpRequest req) throws Exception {
        HttpResponse<String> res = HTTP.send(req,
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400)
            throw new RuntimeException("HTTP " + res.statusCode() + ": " + res.body());
        return res.body();
    }
}
