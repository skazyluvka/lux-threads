package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class Order {
    private int        id;
    @SerializedName("user_id")         private String     userId;
    @SerializedName("total_price")     private BigDecimal totalPrice;
    @SerializedName("discount_amount") private BigDecimal discountAmount;
    @SerializedName("promo_code")      private String     promoCode;
    private String     status;
    @SerializedName("delivery_address") private String    deliveryAddress;
    private String     phone;
    private String     comment;
    @SerializedName("created_at")      private String     createdAt;

    public int        getId()             { return id; }
    public String     getUserId()         { return userId; }
    public BigDecimal getTotalPrice()     { return totalPrice; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String     getPromoCode()      { return promoCode; }
    public String     getStatus()         { return status; }
    public String     getDeliveryAddress(){ return deliveryAddress; }
    public String     getPhone()          { return phone; }
    public String     getComment()        { return comment; }
    public String     getCreatedAt()      { return createdAt; }
    public void       setStatus(String v) { status = v; }

    // For builder pattern
    public void setUserId(String v)          { userId          = v; }
    public void setTotalPrice(BigDecimal v)  { totalPrice      = v; }
    public void setDiscountAmount(BigDecimal v){ discountAmount = v; }
    public void setPromoCode(String v)       { promoCode       = v; }
    public void setDeliveryAddress(String v) { deliveryAddress = v; }
    public void setPhone(String v)           { phone           = v; }
    public void setComment(String v)         { comment         = v; }
}
