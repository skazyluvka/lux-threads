package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class PostComment {

    private long id;

    @SerializedName("post_id")
    private long postId;

    @SerializedName("user_id")
    private String userId;

    private String content;

    @SerializedName("created_at")
    private String createdAt;

    private UserProfile userProfile;

    public long getId() { return id; }
    public long getPostId() { return postId; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

    public String getAuthorName() {
        if (userProfile == null) return "Пользователь";
        String name = userProfile.getFullName();
        return name == null || name.isBlank() ? "Пользователь" : name;
    }
}