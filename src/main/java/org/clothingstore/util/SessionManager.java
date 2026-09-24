package org.clothingstore.util;

import org.clothingstore.model.UserProfile;
import org.clothingstore.model.PromoCode;

public class SessionManager {
    private static String accessToken;
    private static String userId;
    private static String userEmail;
    private static UserProfile profile;
    private static PromoCode appliedPromo;

    public static void login(String token, String uid, String email) {
        accessToken = token;
        userId      = uid;
        userEmail   = email;
    }

    public static void setProfile(UserProfile p) { profile = p; }
    public static UserProfile getProfile()       { return profile; }
    public static String getAccessToken()        { return accessToken; }
    public static String getUserId()             { return userId; }
    public static String getUserEmail()          { return userEmail; }

    public static boolean isLoggedIn()           { return accessToken != null; }

    // ✅ ПРОВЕРКИ РОЛЕЙ
    public static boolean isAdmin() {
        return profile != null && profile.isAdmin();
    }

    public static boolean isSeller() {
        return profile != null && profile.isSeller();
    }

    public static void setAppliedPromo(PromoCode promo) { appliedPromo = promo; }
    public static PromoCode getAppliedPromo()           { return appliedPromo; }

    public static void logout() {
        accessToken = null;
        userId      = null;
        userEmail   = null;
        profile     = null;
        appliedPromo = null;
    }
}