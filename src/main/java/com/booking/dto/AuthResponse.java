package com.booking.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInMs) {

    public static AuthResponse bearer(String token, long expiresInMs) {
        return new AuthResponse(token, "Bearer", expiresInMs);
    }
}
