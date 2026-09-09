package com.booking;

import com.booking.model.Role;
import com.booking.model.User;
import com.booking.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    @Test
    void tokenRoundTrips() {
        User user = user();
        String token = jwtService.generateToken(user);

        assertEquals("user@booking.com", jwtService.extractEmail(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user());

        assertThrows(JwtException.class, () -> jwtService.extractEmail(token + "tampered"));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1000);
        String token = shortLived.generateToken(user());

        assertThrows(ExpiredJwtException.class, () -> shortLived.extractEmail(token));
    }

    private static User user() {
        User user = new User("user@booking.com", "not-a-real-hash", "Arjun Mehta", Role.USER);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }
}
