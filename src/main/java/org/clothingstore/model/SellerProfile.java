package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class SellerProfile {

    private String id;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    private String phone;
    private String address;
    private String role;

    @SerializedName("is_seller")
    private boolean seller;

    @SerializedName("seller_name")
    private String sellerName;

    @SerializedName("seller_bio")
    private String sellerBio;

    @SerializedName("seller_avatar_url")
    private String sellerAvatarUrl;

    public SellerProfile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isSeller() {
        return seller;
    }

    public void setSeller(boolean seller) {
        this.seller = seller;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerBio() {
        return sellerBio;
    }

    public void setSellerBio(String sellerBio) {
        this.sellerBio = sellerBio;
    }

    public String getSellerAvatarUrl() {
        return sellerAvatarUrl;
    }

    public void setSellerAvatarUrl(String sellerAvatarUrl) {
        this.sellerAvatarUrl = sellerAvatarUrl;
    }

    public String getFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String full = (first + " " + last).trim();
        return full;
    }

    public String getDisplayName() {
        if (sellerName != null && !sellerName.isBlank()) {
            return sellerName.trim();
        }

        String full = getFullName();
        if (!full.isBlank()) {
            return full;
        }

        return "Продавец";
    }
}