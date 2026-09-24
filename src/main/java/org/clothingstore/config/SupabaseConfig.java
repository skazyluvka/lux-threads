package org.clothingstore.config;

public class SupabaseConfig {
    // ★ Замени на свои данные
    public static final String PROJECT_URL = "https://oxjehtedkbjumozifdnk.supabase.co";
    public static final String ANON_KEY    = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im94amVodGVka2JqdW1vemlmZG5rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4MTk5MzQsImV4cCI6MjEwNTM5NTkzNH0.hhYnz8zE0tzQMx--bGseFDRkVPdBUpuXihMTMXQOyzE";

    public static final String REST_URL    = PROJECT_URL + "/rest/v1";
    public static final String AUTH_URL    = PROJECT_URL + "/auth/v1";
    public static final String STORAGE_URL = PROJECT_URL + "/storage/v1";
    public static final String BUCKET_NAME = "product-images";

    public static String getImagePublicUrl(String fileName) {
        return STORAGE_URL + "/object/public/" + BUCKET_NAME + "/" + fileName;
    }
}
