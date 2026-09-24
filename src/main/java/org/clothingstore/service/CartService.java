package org.clothingstore.service;

import org.clothingstore.model.CartItem;
import org.clothingstore.model.Product;
import org.clothingstore.model.ProductSize;
import org.clothingstore.util.SessionManager;
import com.google.gson.*;
import java.util.*;

public class CartService extends SupabaseClient {

    private final ProductService productService = new ProductService();

    public List<CartItem> getCart() throws Exception {
        if (!SessionManager.isLoggedIn() || SessionManager.getUserId() == null) {
            return new ArrayList<>();
        }

        String uid = SessionManager.getUserId();
        String res = get("/cart_items?user_id=eq." + uid + "&select=*");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<CartItem> items = new ArrayList<>();
        for (JsonElement el : arr) {
            CartItem item = GSON.fromJson(el, CartItem.class);
            String pRes = get("/products?id=eq." + item.getProductId() + "&select=*");
            JsonArray pArr = GSON.fromJson(pRes, JsonArray.class);
            if (pArr.size() > 0) item.setProduct(GSON.fromJson(pArr.get(0), Product.class));
            items.add(item);
        }
        return items;
    }

    // ✅ НОВЫЙ МЕТОД: проверка наличия на складе
    public int getAvailableQuantity(int productId, String size) throws Exception {
        String res = get("/product_sizes?product_id=eq." + productId + "&size=eq." + size + "&select=quantity");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        if (arr.size() == 0) return 0;
        return arr.get(0).getAsJsonObject().get("quantity").getAsInt();
    }

    // ✅ ИСПРАВЛЕНО: проверка перед добавлением в корзину
    public void addToCart(int productId, String size, int quantity) throws Exception {
        String uid = SessionManager.getUserId();

        // ✅ Проверяем наличие на складе
        int availableQty = getAvailableQuantity(productId, size);
        if (availableQty == 0) {
            throw new Exception("Товар (размер " + size + ") отсутствует на складе");
        }

        // Проверяем, есть ли уже в корзине
        String existing = get("/cart_items?user_id=eq." + uid
                + "&product_id=eq." + productId + "&size=eq." + size);
        JsonArray arr = GSON.fromJson(existing, JsonArray.class);

        if (arr.size() > 0) {
            int curQty = arr.get(0).getAsJsonObject().get("quantity").getAsInt();
            int newTotalQty = curQty + quantity;

            // ✅ Проверяем, не превышает ли общее количество в корзине доступное на складе
            if (newTotalQty > availableQty) {
                throw new Exception("Нельзя добавить больше, чем есть на складе. Доступно: " + availableQty + " шт. (уже в корзине: " + curQty + ")");
            }

            int itemId = arr.get(0).getAsJsonObject().get("id").getAsInt();
            Map<String,Object> body = new HashMap<>();
            body.put("quantity", newTotalQty);
            patch("/cart_items?id=eq." + itemId, body);
        } else {
            // ✅ Проверяем, не превышает ли количество доступное на складе
            if (quantity > availableQty) {
                throw new Exception("Нельзя добавить больше, чем есть на складе. Доступно: " + availableQty + " шт.");
            }

            Map<String,Object> body = new HashMap<>();
            body.put("user_id",    uid);
            body.put("product_id", productId);
            body.put("size",       size);
            body.put("quantity",   quantity);
            post("/cart_items", body);
        }
    }

    public void updateQuantity(int itemId, int newQty) throws Exception {
        if (newQty < 1) {
            removeFromCart(itemId);
            return;
        }

        // Получаем информацию о товаре в корзине
        String res = get("/cart_items?id=eq." + itemId + "&select=product_id,size");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        if (arr.size() == 0) throw new Exception("Товар в корзине не найден");

        int productId = arr.get(0).getAsJsonObject().get("product_id").getAsInt();
        String size = arr.get(0).getAsJsonObject().get("size").getAsString();

        // ✅ Проверяем наличие на складе
        int availableQty = getAvailableQuantity(productId, size);
        if (newQty > availableQty) {
            throw new Exception("Нельзя установить количество больше, чем есть на складе. Доступно: " + availableQty + " шт.");
        }

        Map<String,Object> body = new HashMap<>();
        body.put("quantity", newQty);
        patch("/cart_items?id=eq." + itemId, body);
    }

    public void removeFromCart(int itemId) throws Exception {
        delete("/cart_items?id=eq." + itemId);
    }

    public void clearCart() throws Exception {
        if (!SessionManager.isLoggedIn() || SessionManager.getUserId() == null) return;
        String uid = SessionManager.getUserId();
        delete("/cart_items?user_id=eq." + uid);
    }

    public int getCartCount() throws Exception {
        if (!SessionManager.isLoggedIn() || SessionManager.getUserId() == null) {
            return 0;
        }

        String uid = SessionManager.getUserId();
        String res = get("/cart_items?user_id=eq." + uid + "&select=quantity");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        int count = 0;
        for (JsonElement el : arr) count += el.getAsJsonObject().get("quantity").getAsInt();
        return count;
    }
}