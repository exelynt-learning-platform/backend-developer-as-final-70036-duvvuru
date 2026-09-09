package com.booking;

import com.booking.model.Resource;
import com.booking.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ResourceRepository resources;

    @Test
    void anonymousCannotListResources() throws Exception {
        mvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadResources() throws Exception {
        mvc.perform(get("/api/resources").with(bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)));
    }

    @Test
    void userCannotCreateResource() throws Exception {
        mvc.perform(post("/api/resources")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(validResourceBody())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void userCannotUpdateOrDeleteResource() throws Exception {
        long roomId = resourceId("Meeting Room B");

        mvc.perform(put("/api/resources/" + roomId)
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(validResourceBody())))
                .andExpect(status().isForbidden());

        // use a fresh resource for delete so the 403 isn't masked by the 409 conflict check
        long deletable = createResourceAsAdmin("Whiteboard");
        mvc.perform(delete("/api/resources/" + deletable).with(bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHasFullCrud() throws Exception {
        long id = createResourceAsAdmin("DSLR Camera");

        mvc.perform(get("/api/resources/" + id).with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DSLR Camera"));

        Map<String, Object> update = validResourceBody();
        update.put("name", "DSLR Camera (Canon)");
        mvc.perform(put("/api/resources/" + id)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DSLR Camera (Canon)"));

        mvc.perform(delete("/api/resources/" + id).with(bearer(adminToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/resources/" + id).with(bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsComeBackAs400() throws Exception {
        Map<String, Object> body = validResourceBody();
        body.put("name", "");
        body.put("capacity", -3);

        mvc.perform(post("/api/resources")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.capacity").exists());
    }

    @Test
    void deletingResourceThatHasReservationsConflicts() throws Exception {
        // Meeting Room A has a seeded reservation
        mvc.perform(delete("/api/resources/" + resourceId("Meeting Room A")).with(bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    private Map<String, Object> validResourceBody() {
        return new java.util.HashMap<>(Map.of(
                "name", "Podcast Booth",
                "type", "ROOM",
                "description", "Soundproof booth with two mics",
                "capacity", 2));
    }

    private long createResourceAsAdmin(String name) throws Exception {
        Map<String, Object> body = validResourceBody();
        body.put("name", name);
        MvcResult result = mvc.perform(post("/api/resources")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long resourceId(String name) {
        return resources.findAll().stream()
                .filter(r -> r.getName().equals(name))
                .map(Resource::getId)
                .findFirst()
                .orElseThrow();
    }
}
