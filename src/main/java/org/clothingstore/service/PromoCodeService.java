package org.clothingstore.service;

import org.clothingstore.model.PromoCode;
import com.google.gson.*;
import java.util.*;

public class PromoCodeService extends SupabaseClient {

    public PromoCode validate(String code) throws Exception {
        String res = get("/promo_codes?code=eq." + code + "&is_active=eq.true&select=*");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        if (arr.size() == 0) return null;
        PromoCode pc = GSON.fromJson(arr.get(0), PromoCode.class);
        // Check max uses
        if (pc.getMaxUses() != null && pc.getCurrentUses() >= pc.getMaxUses()) return null;
        return pc;
    }

    public List<PromoCode> getAll() throws Exception {
        String res = get("/promo_codes?select=*&order=id");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<PromoCode> list = new ArrayList<>();
        for (JsonElement el : arr) list.add(GSON.fromJson(el, PromoCode.class));
        return list;
    }

    public PromoCode create(PromoCode pc) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("code",             pc.getCode());
        body.put("discount_percent", pc.getDiscountPercent());
        body.put("is_active",        pc.isActive());
        if (pc.getMaxUses() != null) body.put("max_uses", pc.getMaxUses());
        String res = post("/promo_codes", body);
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        return GSON.fromJson(arr.get(0), PromoCode.class);
    }

    public void toggleActive(int id, boolean active) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("is_active", active);
        patch("/promo_codes?id=eq." + id, body);
    }

    public void delete(int id) throws Exception {
        delete("/promo_codes?id=eq." + id);
    }

    public void incrementUses(int id) throws Exception {
        // fetch current then increment
        String res = get("/promo_codes?id=eq." + id + "&select=current_uses");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        int cur = arr.get(0).getAsJsonObject().get("current_uses").getAsInt();
        Map<String,Object> body = new HashMap<>();
        body.put("current_uses", cur + 1);
        patch("/promo_codes?id=eq." + id, body);
    }
}
