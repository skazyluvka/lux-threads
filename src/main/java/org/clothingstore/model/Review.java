package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class Review {
    private long id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("product_id")
    private int productId;

    private int rating;
    private String comment;

    @SerializedName("created_at")
    private String createdAt;

    private UserProfile userProfile;

    public long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getProductId() {
        return productId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public String getAuthorName() {
        if (userProfile == null) return "Пользователь";
        String name = userProfile.getFullName();
        return name == null || name.isBlank() ? "Пользователь" : name;
    }

    public String getStars() {
        return "★".repeat(Math.max(0, rating)) + "☆".repeat(Math.max(0, 5 - rating));
    }
}