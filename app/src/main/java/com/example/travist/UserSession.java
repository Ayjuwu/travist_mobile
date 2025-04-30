package com.example.travist;

public class UserSession {
    private static int userId;
    private static String token;

    // Constructeur privé pour empêcher toute instanciation
    private UserSession() {}

    // Getters et Setters
    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int userId) {
        UserSession.userId = userId;
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        UserSession.token = token;
    }
}
