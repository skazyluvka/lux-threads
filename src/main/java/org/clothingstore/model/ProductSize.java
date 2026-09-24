package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class ProductSize {
    private int    id;
    @SerializedName("product_id") private int productId;
    private String size;
    private int    quantity;

    public int    getId()        { return id; }
    public int    getProductId() { return productId; }
    public String getSize()      { return size; }
    public int    getQuantity()  { return quantity; }
    public void   setSize(String v)     { size     = v; }
    public void   setQuantity(int v)    { quantity = v; }
    public void   setProductId(int v)   { productId= v; }

    @Override public String toString()  { return size + " (" + quantity + " шт.)"; }
}
