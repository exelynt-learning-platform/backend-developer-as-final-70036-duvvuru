package com.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole app against in-memory H2 and gives subclasses helpers for
 * logging in as one of the seeded users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class IntegrationTestBase {

    static final String ADMIN_EMAIL = "admin@booking.com";
    static final String ADMIN_PASSWORD = "admin123";
    static final String USER_EMAIL = "user@booking.com";
    static final String USER_PASSWORD = "user123";
    static final String USER2_EMAIL = "priya@booking.com";
    static final String USER2_PASSWORD = "user123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    protected String adminToken;
    protected String userToken;
    protected String user2Token;

    @BeforeEach
    void logInUsers() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = login(USER_EMAIL, USER_PASSWORD);
        user2Token = login(USER2_EMAIL, USER2_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    protected static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
