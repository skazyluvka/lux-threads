package org.clothingstore.service;

import org.clothingstore.model.Product;
import org.clothingstore.model.ProductSize;
import com.google.gson.*;

import java.math.BigDecimal;
import java.util.*;

public class ProductService extends SupabaseClient {

    public List<Product> getAll() throws Exception {
        String res = get("/products?select=*&order=id");
        return parseProducts(res);
    }

    public List<Product> getByCategory(int categoryId) throws Exception {
        String res = get("/products?select=*&category_id=eq." + categoryId
                + "&is_active=eq.true&order=id");
        return parseProducts(res);
    }

    public List<ProductSize> getSizes(int productId) throws Exception {
        String res = get("/product_sizes?product_id=eq."
                + productId + "&select=*");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<ProductSize> list = new ArrayList<>();
        for (JsonElement el : arr)
            list.add(GSON.fromJson(el, ProductSize.class));
        return list;
    }

    public Product create(Product p) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("category_id", p.getCategoryId());
        body.put("name",        p.getName());
        body.put("description", p.getDescription());
        body.put("price",       p.getPrice());
        body.put("is_active",   p.isActive());

        // ✅ ДОБАВЛЕНО: передаём seller_id
        if (p.getSellerId() != null && !p.getSellerId().isBlank()) {
            body.put("seller_id", p.getSellerId());
        }

        if (p.getImageUrl() != null && !p.getImageUrl().isBlank())
            body.put("image_url", p.getImageUrl());

        String res = post("/products", body);
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        return GSON.fromJson(arr.get(0), Product.class);
    }

    public void update(Product p) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("category_id", p.getCategoryId());
        body.put("name",        p.getName());
        body.put("description", p.getDescription());
        body.put("price",       p.getPrice());
        body.put("is_active",   p.isActive());
        body.put("image_url",   p.getImageUrl());

        patch("/products?id=eq." + p.getId(), body);
    }

    public void delete(int id) throws Exception {
        delete("/products?id=eq." + id);
    }

    public void setSizeQty(int productId, String size, int qty) throws Exception {
        String existing = get("/product_sizes?product_id=eq."
                + productId + "&size=eq." + size);
        JsonArray arr = GSON.fromJson(existing, JsonArray.class);
        Map<String, Object> body = new HashMap<>();
        body.put("product_id", productId);
        body.put("size",       size);
        body.put("quantity",   qty);
        if (arr.size() == 0) post("/product_sizes", body);
        else patch("/product_sizes?product_id=eq."
                + productId + "&size=eq." + size, body);
    }

    private List<Product> parseProducts(String json) {
        JsonArray arr = GSON.fromJson(json, JsonArray.class);
        List<Product> list = new ArrayList<>();
        for (JsonElement el : arr)
            list.add(GSON.fromJson(el, Product.class));
        return list;
    }
}