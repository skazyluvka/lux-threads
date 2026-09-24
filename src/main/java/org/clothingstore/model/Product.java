package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Product {

    private int id;
    private String name;
    private String description;
    private double price;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("category_id")
    private int categoryId;

    @SerializedName("seller_id")
    private String sellerId;

    private boolean active = true;

    private List<ProductSize> sizes = new ArrayList<>();

    public Product() {}

    public Product(int id, String name, String description, double price,
                   String imageUrl, int categoryId, String sellerId) {
        this.id         = id;
        this.name       = name;
        this.description = description;
        this.price      = price;
        this.imageUrl   = imageUrl;
        this.categoryId = categoryId;
        this.sellerId   = sellerId;
        this.active     = true;
    }

    public int getId()                     { return id; }
    public void setId(int id)              { this.id = id; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public String getDescription()         { return description; }
    public void setDescription(String d)   { this.description = d; }

    public double getPrice()               { return price; }
    public void setPrice(double price)     { this.price = price; }

    public java.math.BigDecimal getPriceBD() {
        return java.math.BigDecimal.valueOf(price);
    }

    public int getPriceInt()               { return (int) Math.round(price); }
    public String getPriceFormatted()      { return getPriceInt() + " ₽"; }

    public String getImageUrl()            { return imageUrl; }
    public void setImageUrl(String url)    { this.imageUrl = url; }

    public int getCategoryId()             { return categoryId; }
    public void setCategoryId(int id)      { this.categoryId = id; }

    public String getSellerId()            { return sellerId; }
    public void setSellerId(String id)     { this.sellerId = id; }

    public boolean isActive()              { return active; }
    public void setActive(boolean active)  { this.active = active; }

    public List<ProductSize> getSizes()    { return sizes; }
    public void setSizes(List<ProductSize> s) {
        this.sizes = s != null ? s : new ArrayList<>();
    }
}