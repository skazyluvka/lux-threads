package org.clothingstore.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.clothingstore.model.Review;
import org.clothingstore.model.UserProfile;
import org.clothingstore.util.SessionManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewService extends SupabaseClient {

    public List<Review> getByProduct(int productId) throws Exception {
        String res = get("/reviews?product_id=eq." + productId + "&select=*&order=created_at.desc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);

        List<Review> list = new ArrayList<>();
        for (JsonElement el : arr) {
            Review review = GSON.fromJson(el, Review.class);
            loadUserProfile(review);
            list.add(review);
        }
        return list;
    }

    public void addReview(int productId, int rating, String comment) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", SessionManager.getUserId());
        body.put("product_id", productId);
        body.put("rating", rating);
        body.put("comment", comment == null ? "" : comment.trim());
        post("/reviews", body);
    }

    public boolean hasUserReview(int productId) throws Exception {
        String uid = SessionManager.getUserId();
        String res = get("/reviews?user_id=eq." + uid + "&product_id=eq." + productId + "&select=id");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        return arr.size() > 0;
    }

    private void loadUserProfile(Review review) {
        try {
            String uid = URLEncoder.encode(review.getUserId(), StandardCharsets.UTF_8);
            String res = get("/user_profiles?id=eq." + uid + "&select=*");
            JsonArray arr = GSON.fromJson(res, JsonArray.class);
            if (arr.size() > 0) {
                UserProfile profile = GSON.fromJson(arr.get(0), UserProfile.class);
                review.setUserProfile(profile);
            }
        } catch (Exception ignored) {
        }
    }
}