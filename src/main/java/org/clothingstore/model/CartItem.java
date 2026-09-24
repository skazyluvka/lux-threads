package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class CartItem {

    private int id;

    @SerializedName("product_id")
    private int productId;

    private String size;
    private int    quantity;

    private transient Product product;

    public CartItem() {}

    public CartItem(int productId, String size, int quantity) {
        this.productId = productId;
        this.size      = size;
        this.quantity  = quantity;
    }

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public int    getProductId()       { return productId; }
    public void   setProductId(int v)  { this.productId = v; }

    public String getSize()            { return size; }
    public void   setSize(String s)    { this.size = s; }

    public int    getQuantity()        { return quantity; }
    public void   setQuantity(int v)   { this.quantity = v; }

    public Product getProduct()        { return product; }
    public void    setProduct(Product p) { this.product = p; }

    public BigDecimal getTotalPrice() {
        if (product == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(product.getPrice())
                .multiply(BigDecimal.valueOf(quantity));
    }
}