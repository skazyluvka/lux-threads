package org.clothingstore.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.clothingstore.model.Product;
import org.clothingstore.model.SellerProfile;

import java.util.ArrayList;
import java.util.List;

public class SellerService extends SupabaseClient {

    public SellerProfile getById(String sellerId) throws Exception {
        String res = get("/user_profiles?id=eq." + sellerId + "&select=*");
        JsonArray arr = JsonParser.parseString(res).getAsJsonArray();

        if (arr.size() == 0) return null;

        return parseSeller(arr.get(0).getAsJsonObject());
    }

    public List<Product> getProductsBySeller(String sellerId) throws Exception {
        String res = get("/products?seller_id=eq." + sellerId + "&order=id.desc&select=*");
        JsonArray arr = JsonParser.parseString(res).getAsJsonArray();

        List<Product> products = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            products.add(parseProduct(obj));
        }
        return products;
    }

    public double getSellerRating(String sellerId) throws Exception {
        String productsRes = get("/products?seller_id=eq." + sellerId + "&select=id");
        JsonArray products = JsonParser.parseString(productsRes).getAsJsonArray();

        if (products.size() == 0) return 0.0;

        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) ids.append(",");
            ids.append(products.get(i).getAsJsonObject().get("id").getAsString());
        }

        String reviewsRes = get("/reviews?product_id=in.(" + ids + ")&select=rating");
        JsonArray reviews = JsonParser.parseString(reviewsRes).getAsJsonArray();

        if (reviews.size() == 0) return 0.0;

        double sum = 0;
        for (JsonElement el : reviews) {
            JsonObject review = el.getAsJsonObject();
            if (review.has("rating") && !review.get("rating").isJsonNull()) {
                sum += review.get("rating").getAsDouble();
            }
        }

        return Math.round((sum / reviews.size()) * 10.0) / 10.0;
    }

    public boolean updateSellerProfile(String sellerId, String sellerName, String bio, String avatarUrl) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("seller_name", sellerName);
        body.addProperty("seller_bio", bio);
        body.addProperty("seller_avatar_url", avatarUrl);
        body.addProperty("is_seller", true);

        patch("/user_profiles?id=eq." + sellerId, body);
        return true;
    }

    public boolean becomeSeller(String userId, String sellerName, String bio, String avatarUrl) throws Exception {
        return updateSellerProfile(userId, sellerName, bio, avatarUrl);
    }

    private SellerProfile parseSeller(JsonObject obj) {
        SellerProfile seller = new SellerProfile();

        seller.setId(getString(obj, "id"));
        seller.setFirstName(getString(obj, "first_name"));
        seller.setLastName(getString(obj, "last_name"));
        seller.setPhone(getString(obj, "phone"));
        seller.setAddress(getString(obj, "address"));
        seller.setRole(getString(obj, "role"));
        seller.setSeller(getBoolean(obj, "is_seller"));
        seller.setSellerName(getString(obj, "seller_name"));
        seller.setSellerBio(getString(obj, "seller_bio"));
        seller.setSellerAvatarUrl(getString(obj, "seller_avatar_url"));

        return seller;
    }

    private Product parseProduct(JsonObject obj) {
        Product p = new Product();

        p.setId(getInt(obj, "id"));
        p.setName(getString(obj, "name"));
        p.setDescription(getString(obj, "description"));
        p.setPrice(getDouble(obj, "price"));
        p.setImageUrl(getString(obj, "image_url"));
        p.setCategoryId(getInt(obj, "category_id"));
        p.setSellerId(getString(obj, "seller_id"));

        // ✅ ИСПРАВЛЕНО: проверяем is_active вместо active
        if (obj.has("is_active") && !obj.get("is_active").isJsonNull()) {
            p.setActive(obj.get("is_active").getAsBoolean());
        } else if (obj.has("active") && !obj.get("active").isJsonNull()) {
            p.setActive(obj.get("active").getAsBoolean());
        }

        return p;
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : null;
    }

    private int getInt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsInt()
                : 0;
    }

    private double getDouble(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsDouble()
                : 0.0;
    }

    private boolean getBoolean(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                && obj.get(key).getAsBoolean();
    }
}