package org.clothingstore.service;

import org.clothingstore.model.CartItem;
import org.clothingstore.model.Order;
import org.clothingstore.model.OrderItem;
import org.clothingstore.util.SessionManager;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

public class OrderService extends SupabaseClient {

    public Order createOrder(Order order, List<CartItem> cartItems) throws Exception {
        // ✅ 1. ПРОВЕРКА НАЛИЧИЯ ТОВАРОВ ПЕРЕД СОЗДАНИЕМ ЗАКАЗА
        for (CartItem item : cartItems) {
            String res = get("/product_sizes?product_id=eq." + item.getProductId()
                    + "&size=eq." + item.getSize() + "&select=quantity");
            JsonArray arr = GSON.fromJson(res, JsonArray.class);

            if (arr.size() == 0) {
                throw new Exception("Товар '" + item.getProduct().getName() + "' (размер " + item.getSize() + ") недоступен.");
            }

            int availableQty = arr.get(0).getAsJsonObject().get("quantity").getAsInt();
            if (availableQty < item.getQuantity()) {
                throw new Exception("Недостаточно товара '" + item.getProduct().getName() + "' (размер " + item.getSize() + "). В наличии: " + availableQty + " шт., а вы пытаетесь заказать: " + item.getQuantity());
            }
        }

        // ✅ 2. СОЗДАНИЕ ЗАКАЗА
        Map<String,Object> body = new HashMap<>();
        body.put("user_id",          order.getUserId());
        body.put("total_price",      order.getTotalPrice());
        body.put("discount_amount",  order.getDiscountAmount());
        body.put("promo_code",       order.getPromoCode());
        body.put("status",           "pending");
        body.put("delivery_address", order.getDeliveryAddress());
        body.put("phone",            order.getPhone());
        body.put("comment",          order.getComment());

        String res = post("/orders", body);
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        Order created = GSON.fromJson(arr.get(0), Order.class);

        // ✅ 3. СОЗДАНИЕ ПОЗИЦИЙ ЗАКАЗА И СПИСАНИЕ СО СКЛАДА
        for (CartItem item : cartItems) {
            // Добавляем позицию в заказ
            Map<String,Object> iBody = new HashMap<>();
            iBody.put("order_id",   created.getId());
            iBody.put("product_id", item.getProductId());
            iBody.put("size",       item.getSize());
            iBody.put("quantity",   item.getQuantity());
            iBody.put("price",      item.getProduct() != null ? item.getProduct().getPrice() : BigDecimal.ZERO);
            post("/order_items", iBody);

            // ✅ СПИСЫВАЕМ ТОВАР СО СКЛАДА
            String sizeRes = get("/product_sizes?product_id=eq." + item.getProductId() + "&size=eq." + item.getSize() + "&select=quantity,id");
            JsonArray sizeArr = GSON.fromJson(sizeRes, JsonArray.class);
            if (sizeArr.size() > 0) {
                int currentQty = sizeArr.get(0).getAsJsonObject().get("quantity").getAsInt();
                int sizeId = sizeArr.get(0).getAsJsonObject().get("id").getAsInt();
                int newQty = currentQty - item.getQuantity();

                Map<String,Object> updateBody = new HashMap<>();
                updateBody.put("quantity", newQty);
                patch("/product_sizes?id=eq." + sizeId, updateBody);
            }
        }

        return created;
    }

    public List<Order> getUserOrders() throws Exception {
        String uid = SessionManager.getUserId();
        String res = get("/orders?user_id=eq." + uid + "&select=*&order=created_at.desc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<Order> list = new ArrayList<>();
        for (JsonElement el : arr) list.add(GSON.fromJson(el, Order.class));
        return list;
    }

    public List<Order> getAllOrders() throws Exception {
        String res = get("/orders?select=*&order=created_at.desc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<Order> list = new ArrayList<>();
        for (JsonElement el : arr) list.add(GSON.fromJson(el, Order.class));
        return list;
    }

    public void updateStatus(int orderId, String status) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("status", status);
        patch("/orders?id=eq." + orderId, body);
    }
}