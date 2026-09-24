package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class PromoCode {
    private int    id;
    private String code;
    @SerializedName("discount_percent") private int     discountPercent;
    @SerializedName("is_active")        private boolean isActive;
    @SerializedName("max_uses")         private Integer maxUses;
    @SerializedName("current_uses")     private int     currentUses;

    public int     getId()              { return id; }
    public String  getCode()            { return code; }
    public int     getDiscountPercent() { return discountPercent; }
    public boolean isActive()           { return isActive; }
    public Integer getMaxUses()         { return maxUses; }
    public int     getCurrentUses()     { return currentUses; }

    public void setCode(String v)           { code            = v; }
    public void setDiscountPercent(int v)   { discountPercent = v; }
    public void setActive(boolean v)        { isActive        = v; }
    public void setMaxUses(Integer v)       { maxUses         = v; }

    @Override public String toString()      { return code + " (-" + discountPercent + "%)"; }
}
