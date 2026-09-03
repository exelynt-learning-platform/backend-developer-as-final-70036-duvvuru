package com.booking;

import com.booking.model.Resource;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private ResourceRepository resources;

    // ---------- creation ----------

    @Test
    void bookingBelongsToTheCallerFromTheToken() throws Exception {
        var body = newReservationBody(resourceId("Meeting Room B"), at(5, 10), at(5, 12), "300.00");

        mvc.perform(post("/api/reservations")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userEmail").value(USER_EMAIL))
                .andExpect(jsonPath("$.resourceName").value("Meeting Room B"))
                .andExpect(jsonPath("$.price").value(300.00));
    }

    @Test
    void doubleBookingSameSlotIsRejected() throws Exception {
        // Meeting Room A is already booked from day+1 10:00-12:00 in the seed data
        var body = newReservationBody(resourceId("Meeting Room A"), at(1, 11), at(1, 13), "500.00");

        mvc.perform(post("/api/reservations")
                        .with(bearer(user2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That resource is already booked for the requested time range"));
    }

    @Test
    void endBeforeStartGets400() throws Exception {
        var body = newReservationBody(resourceId("Meeting Room B"), at(5, 14), at(5, 13), "300.00");

        mvc.perform(post("/api/reservations")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endTime must be after startTime"));
    }

    @Test
    void negativePriceGets400() throws Exception {
        var body = newReservationBody(resourceId("Meeting Room B"), at(5, 14), at(5, 16), "-5");

        mvc.perform(post("/api/reservations")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void missingFieldsGet400() throws Exception {
        mvc.perform(post("/api/reservations")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.resourceId").exists())
                .andExpect(jsonPath("$.fieldErrors.startTime").exists())
                .andExpect(jsonPath("$.fieldErrors.endTime").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void bookingUnknownResourceGets404() throws Exception {
        var body = newReservationBody(999999, at(5, 10), at(5, 11), "100.00");

        mvc.perform(post("/api/reservations")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ---------- ownership / visibility ----------

    @Test
    void userSeesOnlyTheirOwnReservations() throws Exception {
        // Priya makes a booking first
        var body = newReservationBody(resourceId("Epson Projector"), at(6, 10), at(6, 11), "120.00");
        mvc.perform(post("/api/reservations")
                        .with(bearer(user2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Arjun lists: 3 seeded bookings of his, and nothing of Priya's
        mvc.perform(get("/api/reservations?size=50").with(bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].userEmail", everyItem(is(USER_EMAIL))));
    }

    @Test
    void adminSeesEveryonesReservations() throws Exception {
        mvc.perform(get("/api/reservations?size=50").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void userCannotReadSomeoneElsesReservation() throws Exception {
        long priyasReservation = reservations.findAll().stream()
                .filter(r -> r.getUser().getEmail().equals(USER2_EMAIL))
                .findFirst()
                .orElseThrow()
                .getId();

        // 404, and it stays silent about the fact that the reservation exists at all
        mvc.perform(get("/api/reservations/" + priyasReservation).with(bearer(userToken)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/reservations/" + priyasReservation).with(bearer(user2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(USER2_EMAIL));

        mvc.perform(get("/api/reservations/" + priyasReservation).with(bearer(adminToken)))
                .andExpect(status().isOk());
    }

    // ---------- filtering / pagination / sorting ----------

    @Test
    void filterByStatus() throws Exception {
        mvc.perform(get("/api/reservations?status=PENDING&size=50").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))));
    }

    @Test
    void filterByPriceRange() throws Exception {
        mvc.perform(get("/api/reservations?minPrice=400&maxPrice=1000&size=50").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // the 499.00 and 800.00 bookings
    }

    @Test
    void minPriceAboveMaxPriceGets400() throws Exception {
        mvc.perform(get("/api/reservations?minPrice=1000&maxPrice=10").with(bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minPrice cannot be greater than maxPrice"));
    }

    @Test
    void bogusStatusFilterGets400() throws Exception {
        mvc.perform(get("/api/reservations?status=WHATEVER").with(bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginationWorks() throws Exception {
        mvc.perform(get("/api/reservations?page=0&size=2").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/reservations?page=1&size=2").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void sortingWorks() throws Exception {
        mvc.perform(get("/api/reservations?sort=price,desc&size=50").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(1200.50))
                .andExpect(jsonPath("$.content[3].price").value(250.00));

        mvc.perform(get("/api/reservations?sort=price,asc&size=50").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(250.00));
    }

    @Test
    void unknownSortPropertyGets400() throws Exception {
        mvc.perform(get("/api/reservations?sort=notAField,desc").with(bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown sort property: notAField"));
    }

    // ---------- status changes / delete ----------

    @Test
    void adminCanUpdateReservation() throws Exception {
        long id = anyReservationOf(USER_EMAIL);
        Map<String, Object> body = new HashMap<>(Map.of(
                "startTime", iso(at(4, 9)),
                "endTime", iso(at(4, 11)),
                "price", "600.00",
                "status", "CONFIRMED"));

        mvc.perform(put("/api/reservations/" + id)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.price").value(600.00));
    }

    @Test
    void invalidStatusValueGets400() throws Exception {
        long id = anyReservationOf(USER_EMAIL);

        mvc.perform(put("/api/reservations/" + id)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime":"%s","endTime":"%s","price":"100.00","status":"TOTALLY_FINE"}
                                """.formatted(iso(at(4, 9)), iso(at(4, 11)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotUpdateOrDeleteReservations() throws Exception {
        long id = anyReservationOf(USER_EMAIL);

        mvc.perform(put("/api/reservations/" + id)
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "startTime", iso(at(4, 9)),
                                "endTime", iso(at(4, 11)),
                                "price", "600.00",
                                "status", "CONFIRMED"))))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/reservations/" + id).with(bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCancelTheirOwnReservation() throws Exception {
        long id = anyReservationOf(USER_EMAIL);

        mvc.perform(patch("/api/reservations/" + id + "/cancel").with(bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // second cancel is a no-op, not an error
        mvc.perform(patch("/api/reservations/" + id + "/cancel").with(bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void userCannotCancelSomeoneElsesReservation() throws Exception {
        long priyasReservation = anyReservationOf(USER2_EMAIL);

        mvc.perform(patch("/api/reservations/" + priyasReservation + "/cancel").with(bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteReservation() throws Exception {
        long id = anyReservationOf(USER_EMAIL);

        mvc.perform(delete("/api/reservations/" + id).with(bearer(adminToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/reservations/" + id).with(bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousGets401() throws Exception {
        mvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private long resourceId(String name) {
        return resources.findAll().stream()
                .filter(r -> r.getName().equals(name))
                .map(Resource::getId)
                .findFirst()
                .orElseThrow();
    }

    private long anyReservationOf(String email) {
        return reservations.findAll().stream()
                .filter(r -> r.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static Map<String, Object> newReservationBody(long resourceId, LocalDateTime start, LocalDateTime end,
                                                          String price) {
        Map<String, Object> body = new HashMap<>();
        body.put("resourceId", resourceId);
        body.put("startTime", iso(start));
        body.put("endTime", iso(end));
        body.put("price", price);
        return body;
    }

    private static String iso(LocalDateTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static LocalDateTime at(int daysFromNow, int hour) {
        return LocalDateTime.now().plusDays(daysFromNow).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }
}
