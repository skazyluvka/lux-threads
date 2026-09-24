package org.clothingstore.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.clothingstore.model.Post;
import org.clothingstore.model.PostComment;
import org.clothingstore.model.SellerProfile;
import org.clothingstore.model.UserProfile;
import org.clothingstore.util.SessionManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostService extends SupabaseClient {

    public List<Post> getByAuthor(String authorId) throws Exception {
        String uid = URLEncoder.encode(authorId, StandardCharsets.UTF_8);
        String res = get("/posts?author_id=eq." + uid + "&select=*&order=created_at.desc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<Post> list = new ArrayList<>();
        for (JsonElement el : arr) {
            list.add(GSON.fromJson(el, Post.class));
        }
        return list;
    }

    public List<Post> getAll() throws Exception {
        String res = get("/posts?select=*&order=created_at.desc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<Post> list = new ArrayList<>();
        for (JsonElement el : arr) {
            list.add(GSON.fromJson(el, Post.class));
        }
        return list;
    }

    public void createPost(String title, String content, String imageUrl) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("author_id", SessionManager.getUserId());
        body.put("title", title == null ? "" : title.trim());
        body.put("content", content == null ? "" : content.trim());
        if (imageUrl != null && !imageUrl.isBlank()) {
            body.put("image_url", imageUrl.trim());
        }
        post("/posts", body);
    }

    public void deletePost(long postId) throws Exception {
        delete("/posts?id=eq." + postId);
    }

    public List<PostComment> getComments(long postId) throws Exception {
        String res = get("/post_comments?post_id=eq." + postId + "&select=*&order=created_at.asc");
        JsonArray arr = GSON.fromJson(res, JsonArray.class);
        List<PostComment> list = new ArrayList<>();
        for (JsonElement el : arr) {
            PostComment c = GSON.fromJson(el, PostComment.class);
            loadUserProfile(c);
            list.add(c);
        }
        return list;
    }

    public void addComment(long postId, String content) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("post_id", postId);
        body.put("user_id", SessionManager.getUserId());
        body.put("content", content.trim());
        post("/post_comments", body);
    }

    private void loadUserProfile(PostComment comment) {
        try {
            String uid = URLEncoder.encode(comment.getUserId(), StandardCharsets.UTF_8);
            String res = get("/user_profiles?id=eq." + uid + "&select=*");
            JsonArray arr = GSON.fromJson(res, JsonArray.class);
            if (arr.size() > 0) {
                UserProfile profile = GSON.fromJson(arr.get(0), UserProfile.class);
                comment.setUserProfile(profile);
            }
        } catch (Exception ignored) {}
    }
}