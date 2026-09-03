package com.booking.service;

import com.booking.dto.AuthResponse;
import com.booking.dto.LoginRequest;
import com.booking.model.User;
import com.booking.repository.UserRepository;
import com.booking.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository users, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        // throws BadCredentialsException on failure, handled as a 401
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        User user = users.findByEmail(request.email()).orElseThrow();
        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.getExpirationMs());
    }
}
