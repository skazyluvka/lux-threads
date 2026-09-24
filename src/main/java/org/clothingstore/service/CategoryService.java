package org.clothingstore.service;

import org.clothingstore.model.Category;
import com.google.gson.*;
import java.util.*;

public class CategoryService extends SupabaseClient {

    public List<Category> getAll() throws Exception {
        String res = get("/categories?select=*&order=id");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<Category> list = new ArrayList<>();
        for (JsonElement el : arr) list.add(GSON.fromJson(el, Category.class));
        return list;
    }

    public Category create(Category c) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("name",        c.getName());
        body.put("description", c.getDescription());
        String res = post("/categories", body);
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        return GSON.fromJson(arr.get(0), Category.class);
    }

    public void update(Category c) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("name",        c.getName());
        body.put("description", c.getDescription());
        patch("/categories?id=eq." + c.getId(), body);
    }

    public void delete(int id) throws Exception {
        delete("/categories?id=eq." + id);
    }
}
