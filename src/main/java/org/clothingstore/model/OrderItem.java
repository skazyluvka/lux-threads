package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class OrderItem {
    private int        id;
    @SerializedName("order_id")   private int        orderId;
    @SerializedName("product_id") private int        productId;
    private String     size;
    private int        quantity;
    private BigDecimal price;

    public int        getOrderId()   { return orderId; }
    public int        getProductId() { return productId; }
    public String     getSize()      { return size; }
    public int        getQuantity()  { return quantity; }
    public BigDecimal getPrice()     { return price; }

    public void setOrderId(int v)       { orderId   = v; }
    public void setProductId(int v)     { productId = v; }
    public void setSize(String v)       { size      = v; }
    public void setQuantity(int v)      { quantity  = v; }
    public void setPrice(BigDecimal v)  { price     = v; }
}
